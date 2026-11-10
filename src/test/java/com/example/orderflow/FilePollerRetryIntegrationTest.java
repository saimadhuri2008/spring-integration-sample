package com.example.orderflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicInteger;

import com.example.orderflow.transformer.CsvToOrderTransformer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class FilePollerRetryIntegrationTest {

    @BeforeEach
    void cleanOutput() throws IOException {
        cleanDir(Path.of("output/success"));
        cleanDir(Path.of("output/failed"));
        cleanDir(Path.of("input"));
        cleanDir(Path.of("output/summary"));
    }

    private static void cleanDir(Path dir) throws IOException {
        if (Files.exists(dir)) {
            try (var walk = Files.walk(dir)) {
                walk.sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                    });
            }
        }
        Files.createDirectories(dir);
    }

    @Test
    void fileOrderRetriesTransientTransformerFailure() throws Exception {
        var id = "ORD-RETRY-" + System.currentTimeMillis();
        Files.writeString(Path.of("input/" + id + ".csv"), id + ",Retry,Ruler,42.00\n");

        await().atMost(Duration.ofSeconds(15))
            .until(() -> Files.exists(Path.of("output/success/order-" + id + ".txt")));

        assertThat(Files.readString(Path.of("output/success/order-" + id + ".txt"))).contains(id);
        try (var failed = Files.list(Path.of("output/failed"))) {
            assertThat(failed.count()).isEqualTo(0);
        }
    }

    @TestConfiguration
    static class RetryTransformerConfig {
        private final AtomicInteger attempts = new AtomicInteger();

        @Bean
        @Primary
        CsvToOrderTransformer retryingCsvToOrderTransformer() {
            return new CsvToOrderTransformer() {
                @Override
                public com.example.orderflow.model.Order transform(String payload) {
                    if (attempts.getAndIncrement() == 0) {
                        throw new IllegalStateException("transient CSV failure");
                    }
                    return super.transform(payload);
                }
            };
        }
    }
}
