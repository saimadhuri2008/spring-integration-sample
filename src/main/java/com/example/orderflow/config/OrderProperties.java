package com.example.orderflow.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "order")
public class OrderProperties {

    private final Poller poller = new Poller();
    private final Routing routing = new Routing();
    private final Summary summary = new Summary();

    public Poller getPoller() {
        return poller;
    }

    public Routing getRouting() {
        return routing;
    }

    public Summary getSummary() {
        return summary;
    }

    public static class Poller {
        private long delay = 5000;

        public long getDelay() {
            return delay;
        }

        public void setDelay(long delay) {
            this.delay = delay;
        }
    }

    public static class Routing {
        private java.math.BigDecimal expressThreshold = java.math.BigDecimal.valueOf(100);

        public java.math.BigDecimal getExpressThreshold() {
            return expressThreshold;
        }

        public void setExpressThreshold(java.math.BigDecimal expressThreshold) {
            this.expressThreshold = expressThreshold;
        }
    }

    public static class Summary {
        private int batchSize = 10;
        private long groupTimeout = 60000;

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }

        public long getGroupTimeout() {
            return groupTimeout;
        }

        public void setGroupTimeout(long groupTimeout) {
            this.groupTimeout = groupTimeout;
        }
    }
}
