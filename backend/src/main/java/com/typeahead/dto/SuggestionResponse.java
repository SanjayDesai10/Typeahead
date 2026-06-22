package com.typeahead.dto;

import java.util.List;

public class SuggestionResponse {
    private List<SuggestionItem> suggestions;
    private boolean cached;
    private String cacheNode;
    private double latencyMs;

    public SuggestionResponse() {}

    public SuggestionResponse(List<SuggestionItem> suggestions, boolean cached, String cacheNode, double latencyMs) {
        this.suggestions = suggestions;
        this.cached = cached;
        this.cacheNode = cacheNode;
        this.latencyMs = latencyMs;
    }

    public List<SuggestionItem> getSuggestions() { return suggestions; }
    public void setSuggestions(List<SuggestionItem> suggestions) { this.suggestions = suggestions; }

    public boolean isCached() { return cached; }
    public void setCached(boolean cached) { this.cached = cached; }

    public String getCacheNode() { return cacheNode; }
    public void setCacheNode(String cacheNode) { this.cacheNode = cacheNode; }

    public double getLatencyMs() { return latencyMs; }
    public void setLatencyMs(double latencyMs) { this.latencyMs = latencyMs; }

    public static SuggestionResponseBuilder builder() { return new SuggestionResponseBuilder(); }

    public static class SuggestionResponseBuilder {
        private List<SuggestionItem> suggestions;
        private boolean cached;
        private String cacheNode;
        private double latencyMs;

        public SuggestionResponseBuilder suggestions(List<SuggestionItem> suggestions) { this.suggestions = suggestions; return this; }
        public SuggestionResponseBuilder cached(boolean cached) { this.cached = cached; return this; }
        public SuggestionResponseBuilder cacheNode(String cacheNode) { this.cacheNode = cacheNode; return this; }
        public SuggestionResponseBuilder latencyMs(double latencyMs) { this.latencyMs = latencyMs; return this; }

        public SuggestionResponse build() {
            return new SuggestionResponse(suggestions, cached, cacheNode, latencyMs);
        }
    }

    public static class SuggestionItem {
        private String query;
        private long count;
        private double trendingScore;

        public SuggestionItem() {}

        public SuggestionItem(String query, long count, double trendingScore) {
            this.query = query;
            this.count = count;
            this.trendingScore = trendingScore;
        }

        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }

        public long getCount() { return count; }
        public void setCount(long count) { this.count = count; }

        public double getTrendingScore() { return trendingScore; }
        public void setTrendingScore(double trendingScore) { this.trendingScore = trendingScore; }

        public static SuggestionItemBuilder builder() { return new SuggestionItemBuilder(); }

        public static class SuggestionItemBuilder {
            private String query;
            private long count;
            private double trendingScore;

            public SuggestionItemBuilder query(String query) { this.query = query; return this; }
            public SuggestionItemBuilder count(long count) { this.count = count; return this; }
            public SuggestionItemBuilder trendingScore(double trendingScore) { this.trendingScore = trendingScore; return this; }

            public SuggestionItem build() { return new SuggestionItem(query, count, trendingScore); }
        }
    }
}
