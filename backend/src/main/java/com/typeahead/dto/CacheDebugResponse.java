package com.typeahead.dto;

import java.util.List;

public class CacheDebugResponse {
    private String prefix;
    private String hashValue;
    private String assignedNode;
    private int ringPosition;
    private boolean cacheHit;
    private int cachedEntries;
    private List<NodeStats> allNodes;

    public CacheDebugResponse() {}

    public CacheDebugResponse(String prefix, String hashValue, String assignedNode,
                               int ringPosition, boolean cacheHit, int cachedEntries,
                               List<NodeStats> allNodes) {
        this.prefix = prefix;
        this.hashValue = hashValue;
        this.assignedNode = assignedNode;
        this.ringPosition = ringPosition;
        this.cacheHit = cacheHit;
        this.cachedEntries = cachedEntries;
        this.allNodes = allNodes;
    }

    public String getPrefix() { return prefix; }
    public void setPrefix(String prefix) { this.prefix = prefix; }
    public String getHashValue() { return hashValue; }
    public void setHashValue(String hashValue) { this.hashValue = hashValue; }
    public String getAssignedNode() { return assignedNode; }
    public void setAssignedNode(String assignedNode) { this.assignedNode = assignedNode; }
    public int getRingPosition() { return ringPosition; }
    public void setRingPosition(int ringPosition) { this.ringPosition = ringPosition; }
    public boolean isCacheHit() { return cacheHit; }
    public void setCacheHit(boolean cacheHit) { this.cacheHit = cacheHit; }
    public int getCachedEntries() { return cachedEntries; }
    public void setCachedEntries(int cachedEntries) { this.cachedEntries = cachedEntries; }
    public List<NodeStats> getAllNodes() { return allNodes; }
    public void setAllNodes(List<NodeStats> allNodes) { this.allNodes = allNodes; }

    public static CacheDebugResponseBuilder builder() { return new CacheDebugResponseBuilder(); }

    public static class CacheDebugResponseBuilder {
        private String prefix;
        private String hashValue;
        private String assignedNode;
        private int ringPosition;
        private boolean cacheHit;
        private int cachedEntries;
        private List<NodeStats> allNodes;

        public CacheDebugResponseBuilder prefix(String prefix) { this.prefix = prefix; return this; }
        public CacheDebugResponseBuilder hashValue(String hashValue) { this.hashValue = hashValue; return this; }
        public CacheDebugResponseBuilder assignedNode(String assignedNode) { this.assignedNode = assignedNode; return this; }
        public CacheDebugResponseBuilder ringPosition(int ringPosition) { this.ringPosition = ringPosition; return this; }
        public CacheDebugResponseBuilder cacheHit(boolean cacheHit) { this.cacheHit = cacheHit; return this; }
        public CacheDebugResponseBuilder cachedEntries(int cachedEntries) { this.cachedEntries = cachedEntries; return this; }
        public CacheDebugResponseBuilder allNodes(List<NodeStats> allNodes) { this.allNodes = allNodes; return this; }

        public CacheDebugResponse build() {
            return new CacheDebugResponse(prefix, hashValue, assignedNode, ringPosition, cacheHit, cachedEntries, allNodes);
        }
    }

    public static class NodeStats {
        private String id;
        private long hits;
        private long misses;
        private double hitRate;
        private long keyCount;

        public NodeStats() {}

        public NodeStats(String id, long hits, long misses, double hitRate, long keyCount) {
            this.id = id;
            this.hits = hits;
            this.misses = misses;
            this.hitRate = hitRate;
            this.keyCount = keyCount;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public long getHits() { return hits; }
        public void setHits(long hits) { this.hits = hits; }
        public long getMisses() { return misses; }
        public void setMisses(long misses) { this.misses = misses; }
        public double getHitRate() { return hitRate; }
        public void setHitRate(double hitRate) { this.hitRate = hitRate; }
        public long getKeyCount() { return keyCount; }
        public void setKeyCount(long keyCount) { this.keyCount = keyCount; }

        public static NodeStatsBuilder builder() { return new NodeStatsBuilder(); }

        public static class NodeStatsBuilder {
            private String id;
            private long hits;
            private long misses;
            private double hitRate;
            private long keyCount;

            public NodeStatsBuilder id(String id) { this.id = id; return this; }
            public NodeStatsBuilder hits(long hits) { this.hits = hits; return this; }
            public NodeStatsBuilder misses(long misses) { this.misses = misses; return this; }
            public NodeStatsBuilder hitRate(double hitRate) { this.hitRate = hitRate; return this; }
            public NodeStatsBuilder keyCount(long keyCount) { this.keyCount = keyCount; return this; }

            public NodeStats build() { return new NodeStats(id, hits, misses, hitRate, keyCount); }
        }
    }
}
