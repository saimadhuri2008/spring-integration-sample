package com.example.orderflow.config;

import java.io.File;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.UUID;

import com.example.orderflow.model.Order;
import com.example.orderflow.service.OrderProcessingService;
import com.example.orderflow.service.OrderStore;
import com.example.orderflow.service.OrderSummaryWriter;
import com.example.orderflow.service.OrderValidationService;
import com.example.orderflow.transformer.CsvToOrderTransformer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.file.dsl.Files;
import org.springframework.integration.file.transformer.FileToStringTransformer;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.handler.advice.RequestHandlerRetryAdvice;
import org.springframework.integration.store.SimpleMessageStore;
import org.springframework.messaging.Message;

@Configuration
public class FileIntegrationConfig {

    private static final Logger log = LoggerFactory.getLogger(FileIntegrationConfig.class);

    private final OrderProperties orderProperties;

    public FileIntegrationConfig(OrderProperties orderProperties) {
        this.orderProperties = orderProperties;
    }

    @Bean
    public IntegrationFlow fileOrderFlow(
            CsvToOrderTransformer csvTransformer,
            OrderValidationService validator,
            OrderProcessingService processor,
            OrderStore orderStore) {

        return IntegrationFlow.from(
                Files.inboundAdapter(new File("input"))
                        .preventDuplicates(true)
                        .patternFilter("*.csv"),
                e -> e.poller(Pollers.fixedDelay(orderProperties.getPoller().getDelay())
                        .errorChannel("errorChannel"))
            )
            .enrichHeaders(h -> h.headerFunction("correlationId", m -> UUID.randomUUID().toString()))
            .log(LoggingHandler.Level.INFO, "orderflow.file")
            .transform(new FileToStringTransformer())
            .handle(csvTransformer, "transform", e -> e.advice(filePollerRetryAdvice()))
            .handle(validator, "validate", e -> e.advice(filePollerRetryAdvice()))
            .route(Order.class, this::isExpressOrder,
                m -> m
                    .subFlowMapping(true, sf -> sf
                        .handle(processor, "processExpress", e -> e.advice(filePollerRetryAdvice()))
                    )
                    .subFlowMapping(false, sf -> sf
                        .handle(processor, "processReview", e -> e.advice(filePollerRetryAdvice()))
                    )
            )
            .handle(orderStore, "put", e -> e.advice(filePollerRetryAdvice()))
            .publishSubscribeChannel(pubSub -> pubSub
                .subscribe(sub -> sub.channel("archiveChannel"))
                .subscribe(sub -> sub.channel("summaryChannel"))
            )
            .get();
    }

    @Bean
    public IntegrationFlow archiveFlow() {
        return IntegrationFlow.from("archiveChannel")
            .enrichHeaders(h -> h.headerExpression("orderId", "payload.id()"))
            .<Order, String>transform(o -> "Order " + o.id() + " | Customer: " + o.customer()
                + " | Description: " + o.description() + " | Total: $" + o.total())
            .handle(Files.outboundAdapter(new File("output/success"))
                    .fileNameGenerator(m -> "order-" + m.getHeaders().get("orderId") + ".txt")
                    .autoCreateDirectory(true))
            .get();
    }

    @Bean
    public IntegrationFlow errorFlow(OrderStore orderStore) {
        return IntegrationFlow.from("errorChannel")
            .handle((payload, headers) -> {
                var throwable = (Throwable) payload;
                var cause = throwable.getCause();
                var message = cause != null ? cause.getMessage() : throwable.getMessage();

                if (throwable instanceof org.springframework.messaging.MessagingException me) {
                    var failedMsg = me.getFailedMessage();
                    if (failedMsg != null && failedMsg.getPayload() instanceof Order order) {
                        orderStore.markFailed(order.id());
                    }
                }

                try {
                    java.nio.file.Files.writeString(
                        new java.io.File("output/failed", "error-" + System.currentTimeMillis() + ".txt").toPath(),
                        "FAILED: " + message);
                } catch (java.io.IOException ex) {
                    log.error("Failed to write file-processing error log", ex);
                }

                var originalFile = headers.get("file_originalFile");
                if (originalFile instanceof String path) {
                    moveFailedFile(path);
                }
                return null;
            })
            .get();
    }

    @Bean
    public IntegrationFlow summaryFlow(OrderSummaryWriter summaryWriter, SimpleMessageStore orderSummaryStore) {
        return IntegrationFlow.from("summaryChannel")
            .enrichHeaders(h -> h
                .headerFunction("summaryKey", m -> {
                    return summaryKey(m);
                })
                .headerFunction("correlationId", m -> {
                    return summaryKey(m);
                }))
            .aggregate(a -> a
                .messageStore(orderSummaryStore)
                .releaseStrategy(g -> g.size() >= orderProperties.getSummary().getBatchSize())
                .groupTimeout(orderProperties.getSummary().getGroupTimeout())
                .sendPartialResultOnExpiry(true)
                .expireGroupsUponCompletion(true))
            .log(LoggingHandler.Level.INFO, "orderflow.summary")
            .handle(summaryWriter, "write")
            .channel("nullChannel")
            .get();
    }

    private boolean isExpressOrder(Order order) {
        return order.total().compareTo(orderProperties.getRouting().getExpressThreshold()) <= 0;
    }

    @Bean
    public RequestHandlerRetryAdvice filePollerRetryAdvice() {
        return new RequestHandlerRetryAdvice();
    }

    private static String summaryKey(Message<?> message) {
        var batch = message.getHeaders().get("summaryBatch");
        if (batch != null && !batch.toString().isBlank()) {
            return batch.toString();
        }
        return LocalDate.now().toString();
    }

    private static void moveFailedFile(String sourcePath) {
        var source = new java.io.File(sourcePath).toPath();
        var targetDir = new java.io.File("input/failed").toPath();
        var target = targetDir.resolve(source.getFileName());
        try {
            java.nio.file.Files.createDirectories(targetDir);
            java.nio.file.Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (java.io.IOException ex) {
            log.error("Failed to move failed input file {} to {}", source, target, ex);
        }
    }
}
