package com.typeahead.dto;

public class MetricsResponse {
    private long totalSearchesReceived;
    private long totalDbWrites;
    private double writeReductionRatio;
    private int currentBufferSize;
    private long flushCount;
    private String lastFlushAt;
    private long totalCacheHits;
    private long totalCacheMisses;
    private double overallHitRate;
    private long totalQueries;
    private long totalDbReads;
    private double avgLatencyMs;
    private double p95LatencyMs;
    private double p99LatencyMs;

    public MetricsResponse() {}

    public MetricsResponse(long totalSearchesReceived, long totalDbWrites, double writeReductionRatio,
                           int currentBufferSize, long flushCount, String lastFlushAt,
                           long totalCacheHits, long totalCacheMisses, double overallHitRate,
                           long totalQueries, long totalDbReads,
                           double avgLatencyMs, double p95LatencyMs, double p99LatencyMs) {
        this.totalSearchesReceived = totalSearchesReceived;
        this.totalDbWrites = totalDbWrites;
        this.writeReductionRatio = writeReductionRatio;
        this.currentBufferSize = currentBufferSize;
        this.flushCount = flushCount;
        this.lastFlushAt = lastFlushAt;
        this.totalCacheHits = totalCacheHits;
        this.totalCacheMisses = totalCacheMisses;
        this.overallHitRate = overallHitRate;
        this.totalQueries = totalQueries;
        this.totalDbReads = totalDbReads;
        this.avgLatencyMs = avgLatencyMs;
        this.p95LatencyMs = p95LatencyMs;
        this.p99LatencyMs = p99LatencyMs;
    }

    // Getters and setters
    public long getTotalSearchesReceived() { return totalSearchesReceived; }
    public void setTotalSearchesReceived(long v) { this.totalSearchesReceived = v; }
    public long getTotalDbWrites() { return totalDbWrites; }
    public void setTotalDbWrites(long v) { this.totalDbWrites = v; }
    public double getWriteReductionRatio() { return writeReductionRatio; }
    public void setWriteReductionRatio(double v) { this.writeReductionRatio = v; }
    public int getCurrentBufferSize() { return currentBufferSize; }
    public void setCurrentBufferSize(int v) { this.currentBufferSize = v; }
    public long getFlushCount() { return flushCount; }
    public void setFlushCount(long v) { this.flushCount = v; }
    public String getLastFlushAt() { return lastFlushAt; }
    public void setLastFlushAt(String v) { this.lastFlushAt = v; }
    public long getTotalCacheHits() { return totalCacheHits; }
    public void setTotalCacheHits(long v) { this.totalCacheHits = v; }
    public long getTotalCacheMisses() { return totalCacheMisses; }
    public void setTotalCacheMisses(long v) { this.totalCacheMisses = v; }
    public double getOverallHitRate() { return overallHitRate; }
    public void setOverallHitRate(double v) { this.overallHitRate = v; }
    public long getTotalQueries() { return totalQueries; }
    public void setTotalQueries(long v) { this.totalQueries = v; }
    public long getTotalDbReads() { return totalDbReads; }
    public void setTotalDbReads(long v) { this.totalDbReads = v; }
    public double getAvgLatencyMs() { return avgLatencyMs; }
    public void setAvgLatencyMs(double v) { this.avgLatencyMs = v; }
    public double getP95LatencyMs() { return p95LatencyMs; }
    public void setP95LatencyMs(double v) { this.p95LatencyMs = v; }
    public double getP99LatencyMs() { return p99LatencyMs; }
    public void setP99LatencyMs(double v) { this.p99LatencyMs = v; }

    public static MetricsResponseBuilder builder() { return new MetricsResponseBuilder(); }

    public static class MetricsResponseBuilder {
        private long totalSearchesReceived;
        private long totalDbWrites;
        private double writeReductionRatio;
        private int currentBufferSize;
        private long flushCount;
        private String lastFlushAt;
        private long totalCacheHits;
        private long totalCacheMisses;
        private double overallHitRate;
        private long totalQueries;
        private long totalDbReads;
        private double avgLatencyMs;
        private double p95LatencyMs;
        private double p99LatencyMs;

        public MetricsResponseBuilder totalSearchesReceived(long v) { this.totalSearchesReceived = v; return this; }
        public MetricsResponseBuilder totalDbWrites(long v) { this.totalDbWrites = v; return this; }
        public MetricsResponseBuilder writeReductionRatio(double v) { this.writeReductionRatio = v; return this; }
        public MetricsResponseBuilder currentBufferSize(int v) { this.currentBufferSize = v; return this; }
        public MetricsResponseBuilder flushCount(long v) { this.flushCount = v; return this; }
        public MetricsResponseBuilder lastFlushAt(String v) { this.lastFlushAt = v; return this; }
        public MetricsResponseBuilder totalCacheHits(long v) { this.totalCacheHits = v; return this; }
        public MetricsResponseBuilder totalCacheMisses(long v) { this.totalCacheMisses = v; return this; }
        public MetricsResponseBuilder overallHitRate(double v) { this.overallHitRate = v; return this; }
        public MetricsResponseBuilder totalQueries(long v) { this.totalQueries = v; return this; }
        public MetricsResponseBuilder totalDbReads(long v) { this.totalDbReads = v; return this; }
        public MetricsResponseBuilder avgLatencyMs(double v) { this.avgLatencyMs = v; return this; }
        public MetricsResponseBuilder p95LatencyMs(double v) { this.p95LatencyMs = v; return this; }
        public MetricsResponseBuilder p99LatencyMs(double v) { this.p99LatencyMs = v; return this; }

        public MetricsResponse build() {
            return new MetricsResponse(totalSearchesReceived, totalDbWrites, writeReductionRatio,
                    currentBufferSize, flushCount, lastFlushAt, totalCacheHits, totalCacheMisses,
                    overallHitRate, totalQueries, totalDbReads, avgLatencyMs, p95LatencyMs, p99LatencyMs);
        }
    }
}
