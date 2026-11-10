package com.example.orderflow.config;

import java.io.File;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.config.GlobalChannelInterceptor;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.graph.IntegrationGraphServer;
import org.springframework.integration.store.SimpleMessageStore;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;

@Configuration
public class IntegrationCommonConfig {

    private static final Logger log = LoggerFactory.getLogger(IntegrationCommonConfig.class);

    @PostConstruct
    public void initDirs() {
        new File("input/failed").mkdirs();
        new File("output/success").mkdirs();
        new File("output/failed").mkdirs();
        new File("output/summary").mkdirs();
        log.debug("Created integration directories");
    }

    @Bean
    public MessageChannel httpOrderInputChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel getOrderInputChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel archiveChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel summaryChannel() {
        return new DirectChannel();
    }

    @Bean
    public SimpleMessageStore orderSummaryStore() {
        return new SimpleMessageStore();
    }

    @Bean
    public IntegrationGraphServer integrationGraphServer() {
        var server = new IntegrationGraphServer();
        server.setApplicationName("order-flow");
        return server;
    }

    @Bean
    @GlobalChannelInterceptor(patterns = "*")
    public ChannelInterceptor correlationIdMdcInterceptor() {
        return new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                Object correlationId = message.getHeaders().get("correlationId");
                if (correlationId != null) {
                    MDC.put("correlationId", correlationId.toString());
                }
                return message;
            }

            @Override
            public void afterSendCompletion(Message<?> message, MessageChannel channel, boolean sent, Exception ex) {
                MDC.remove("correlationId");
            }
        };
    }
}
