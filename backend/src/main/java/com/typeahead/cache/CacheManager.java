package com.typeahead.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typeahead.dto.CacheDebugResponse;
import com.typeahead.dto.SuggestionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Manages distributed cache across multiple Redis logical databases.
 * Uses consistent hashing to route prefix keys to specific Redis nodes.
 */
@Component
public class CacheManager {

    private static final Logger log = LoggerFactory.getLogger(CacheManager.class);

    private final ConsistentHashRing hashRing;
    private final Map<String, StringRedisTemplate> redisTemplates;
    private final ObjectMapper objectMapper;
    private final int ttlSeconds;

    private final Map<String, AtomicLong> hitCounters = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> missCounters = new ConcurrentHashMap<>();
    private final AtomicLong totalDbReads = new AtomicLong(0);

    private static final String[] NODE_IDS = {"redis-node-0", "redis-node-1", "redis-node-2"};

    public CacheManager(ConsistentHashRing hashRing,
                        @Qualifier("redisNodeTemplates") Map<String, StringRedisTemplate> redisTemplates,
                        ObjectMapper objectMapper) {
        this.hashRing = hashRing;
        this.redisTemplates = redisTemplates;
        this.objectMapper = objectMapper;
        this.ttlSeconds = 60;
    }

    @PostConstruct
    public void init() {
        for (String nodeId : NODE_IDS) {
            hashRing.addNode(nodeId);
            hitCounters.put(nodeId, new AtomicLong(0));
            missCounters.put(nodeId, new AtomicLong(0));
        }
        log.info("Cache manager initialized with {} nodes, ring size: {}",
                NODE_IDS.length, hashRing.getRingSize());
    }

    public CacheResult get(String prefix) {
        String nodeId = hashRing.getNode(prefix);
        StringRedisTemplate template = redisTemplates.get(nodeId);
        if (template == null) {
            log.warn("No Redis template found for node: {}", nodeId);
            return null;
        }

        String cacheKey = "suggest:" + prefix.toLowerCase();
        String cached = template.opsForValue().get(cacheKey);

        if (cached != null) {
            hitCounters.get(nodeId).incrementAndGet();
            log.debug("Cache HIT for prefix '{}' on node '{}'", prefix, nodeId);
            try {
                List<SuggestionResponse.SuggestionItem> items =
                        objectMapper.readValue(cached, new TypeReference<>() {});
                return new CacheResult(items, true, nodeId);
            } catch (JsonProcessingException e) {
                log.error("Failed to deserialize cached value for prefix: {}", prefix, e);
                return null;
            }
        }

        missCounters.get(nodeId).incrementAndGet();
        log.debug("Cache MISS for prefix '{}' on node '{}'", prefix, nodeId);
        return new CacheResult(null, false, nodeId);
    }

    public void put(String prefix, List<SuggestionResponse.SuggestionItem> items) {
        String nodeId = hashRing.getNode(prefix);
        StringRedisTemplate template = redisTemplates.get(nodeId);
        if (template == null) return;

        String cacheKey = "suggest:" + prefix.toLowerCase();
        try {
            String json = objectMapper.writeValueAsString(items);
            template.opsForValue().set(cacheKey, json, Duration.ofSeconds(ttlSeconds));
            log.debug("Cached {} suggestions for prefix '{}' on node '{}'", items.size(), prefix, nodeId);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize suggestions for caching: {}", prefix, e);
        }
    }

    public void invalidateForQuery(String query) {
        String q = query.toLowerCase();
        int invalidated = 0;
        for (int i = 1; i <= q.length(); i++) {
            String prefix = q.substring(0, i);
            String nodeId = hashRing.getNode(prefix);
            StringRedisTemplate template = redisTemplates.get(nodeId);
            if (template != null) {
                String cacheKey = "suggest:" + prefix;
                Boolean deleted = template.delete(cacheKey);
                if (Boolean.TRUE.equals(deleted)) {
                    invalidated++;
                }
            }
        }
        if (invalidated > 0) {
            log.debug("Invalidated {} cache entries for query '{}'", invalidated, query);
        }
    }

    public CacheDebugResponse getDebugInfo(String prefix) {
        ConsistentHashRing.DebugInfo debugInfo = hashRing.getDebugInfo(prefix);
        String nodeId = debugInfo.assignedNode();
        StringRedisTemplate template = redisTemplates.get(nodeId);

        boolean cacheHit = false;
        int cachedEntries = 0;
        if (template != null) {
            String cacheKey = "suggest:" + prefix.toLowerCase();
            String cached = template.opsForValue().get(cacheKey);
            if (cached != null) {
                cacheHit = true;
                try {
                    List<?> items = objectMapper.readValue(cached, List.class);
                    cachedEntries = items.size();
                } catch (JsonProcessingException e) { /* ignore */ }
            }
        }

        List<CacheDebugResponse.NodeStats> allNodeStats = new ArrayList<>();
        for (String nid : NODE_IDS) {
            long hits = hitCounters.getOrDefault(nid, new AtomicLong(0)).get();
            long misses = missCounters.getOrDefault(nid, new AtomicLong(0)).get();
            double hitRate = (hits + misses) > 0 ? (double) hits / (hits + misses) : 0.0;

            StringRedisTemplate t = redisTemplates.get(nid);
            long keyCount = 0;
            if (t != null) {
                try {
                    Set<String> keys = t.keys("suggest:*");
                    keyCount = keys != null ? keys.size() : 0;
                } catch (Exception e) { /* ignore */ }
            }

            allNodeStats.add(CacheDebugResponse.NodeStats.builder()
                    .id(nid).hits(hits).misses(misses)
                    .hitRate(Math.round(hitRate * 1000.0) / 1000.0)
                    .keyCount(keyCount).build());
        }

        return CacheDebugResponse.builder()
                .prefix(prefix).hashValue(debugInfo.hashValue())
                .assignedNode(debugInfo.assignedNode())
                .ringPosition(debugInfo.ringPosition())
                .cacheHit(cacheHit).cachedEntries(cachedEntries)
                .allNodes(allNodeStats).build();
    }

    /**
     * Flushes all Redis caches and resets hit/miss stats.
     */
    public void flushAllCaches() {
        for (Map.Entry<String, StringRedisTemplate> entry : redisTemplates.entrySet()) {
            String nodeId = entry.getKey();
            StringRedisTemplate template = entry.getValue();
            if (template != null) {
                try {
                    Objects.requireNonNull(template.getConnectionFactory())
                            .getConnection()
                            .serverCommands()
                            .flushDb();
                    log.info("Flushed Redis cache node: {}", nodeId);
                } catch (Exception e) {
                    log.error("Failed to flush Redis cache node: {}", nodeId, e);
                }
            }
        }
        hitCounters.values().forEach(counter -> counter.set(0));
        missCounters.values().forEach(counter -> counter.set(0));
        totalDbReads.set(0);
        log.info("All cache statistics and nodes have been cleared.");
    }

    public long getTotalHits() {
        return hitCounters.values().stream().mapToLong(AtomicLong::get).sum();
    }

    public long getTotalMisses() {
        return missCounters.values().stream().mapToLong(AtomicLong::get).sum();
    }

    public long getTotalDbReads() {
        return totalDbReads.get();
    }

    public void incrementDbReads() {
        totalDbReads.incrementAndGet();
    }

    public record CacheResult(List<SuggestionResponse.SuggestionItem> items, boolean hit, String nodeId) {}
}
