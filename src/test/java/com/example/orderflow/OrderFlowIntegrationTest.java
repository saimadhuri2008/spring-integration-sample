package com.example.orderflow;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.Duration;
import java.util.Comparator;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class OrderFlowIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

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
    void httpExpressOrder_returnsOkAndWritesFile() {
        var body = """
            {"id":"ORD-001","customer":"Alice","description":"Book","total":25.00}
            """;

        var response = rest.postForEntity("/api/orders", body, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("Order ORD-001 submitted (EXPRESS)");

        await().atMost(Duration.ofSeconds(3))
            .until(() -> Files.exists(Path.of("output/success/order-ORD-001.txt")));
    }

    @Test
    void httpReviewOrder_returnsOkAndWritesFile() {
        var body = """
            {"id":"ORD-002","customer":"Bob","description":"Laptop","total":1500.00}
            """;

        var response = rest.postForEntity("/api/orders", body, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("Order ORD-002 submitted (REVIEW)");

        await().atMost(Duration.ofSeconds(3))
            .until(() -> Files.exists(Path.of("output/success/order-ORD-002.txt")));
    }

    @Test
    void httpInvalidOrder_returns400() {
        var body = """
            {"id":"","customer":"","total":-5}
            """;

        var response = rest.postForEntity("/api/orders", body, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("Order id must not be blank");
    }

    @Test
    void httpDuplicateOrder_returns409() {
        var body = """
            {"id":"ORD-DUP","customer":"Alice","description":"Book","total":25.00}
            """;

        var firstResponse = rest.postForEntity("/api/orders", body, String.class);
        var secondResponse = rest.postForEntity("/api/orders", body, String.class);

        assertThat(firstResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(secondResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(secondResponse.getBody()).contains("Order already exists: ORD-DUP");
    }

    @Test
    void httpListOrders_returnsStoredOrders() {
        var first = """
            {"id":"ORD-LIST-1","customer":"Alice","description":"Book","total":25.00}
            """;
        var second = """
            {"id":"ORD-LIST-2","customer":"Bob","description":"Lamp","total":125.00}
            """;

        rest.postForEntity("/api/orders", first, String.class);
        rest.postForEntity("/api/orders", second, String.class);

        var response = rest.getForEntity("/api/orders", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("ORD-LIST-1").contains("ORD-LIST-2");
    }

    @Test
    void summaryFlow_releasesAfterTenOrders() {
        var batch = "test-" + UUID.randomUUID();
        for (int i = 1; i <= 10; i++) {
            var body = """
                {"id":"ORD-SUM-%d","customer":"Customer %d","description":"Item %d","total":25.00}
                """.formatted(i, i, i);
            var headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("summaryBatch", batch);
            rest.postForEntity("/api/orders", new HttpEntity<>(body, headers), String.class);
        }

        var summaryPath = Path.of("output/summary/summary-" + batch + ".csv");

        await().atMost(Duration.ofSeconds(15))
            .until(() -> Files.exists(summaryPath));

        try {
            assertThat(Files.readString(summaryPath)).contains("ORD-SUM-1").contains("ORD-SUM-10");
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Test
    void summaryFlow_releasesPartialBatchAfterTimeout() {
        var batch = "timeout-" + UUID.randomUUID();
        var body = """
            {"id":"ORD-TIMEOUT","customer":"Timeout","description":"Item","total":25.00}
            """;
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("summaryBatch", batch);

        var response = rest.postForEntity("/api/orders", new HttpEntity<>(body, headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        var summaryPath = Path.of("output/summary/summary-" + batch + ".csv");
        await().atMost(Duration.ofSeconds(5))
            .until(() -> Files.exists(summaryPath));

        try {
            assertThat(Files.readString(summaryPath)).contains("ORD-TIMEOUT");
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Test
    void httpGetOrder_returnsStoredOrder() {
        var postBody = """
            {"id":"ORD-GET","customer":"Charlie","description":"Monitor","total":200.00}
            """;
        rest.postForEntity("/api/orders", postBody, String.class);

        var response = rest.getForEntity("/api/orders/ORD-GET", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("ORD-GET").contains("Charlie").contains("REVIEWED");
    }

    @Test
    void httpGetUnknownOrder_returns404() {
        var response = rest.getForEntity("/api/orders/UNKNOWN", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void integrationGraph_returnsNodesAndLinks() {
        var response = rest.getForEntity("/api/integration/graph", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("nodes").contains("links").contains("contentDescriptor");
        assertThat(response.getBody()).contains("httpOrderFlow").contains("fileOrderFlow").contains("summaryFlow");
    }

    @Test
    void fileOrderPolled_processesAndWritesOutput() throws Exception {
        var id = "ORD-" + System.currentTimeMillis();
        var csv = id + ",David,Keyboard,75.00\n";
        Files.writeString(Path.of("input/" + id + ".csv"), csv);

        await().atMost(Duration.ofSeconds(15))
            .until(() -> Files.exists(Path.of("output/success/order-" + id + ".txt")));

        var output = Files.readString(Path.of("output/success/order-" + id + ".txt"));
        assertThat(output).contains(id);
    }
}
