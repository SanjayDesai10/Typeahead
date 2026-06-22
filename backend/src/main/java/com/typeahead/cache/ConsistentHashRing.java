package com.typeahead.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Consistent Hash Ring implementation for distributing cache keys across Redis nodes.
 * Uses MD5 hashing with configurable virtual nodes for even distribution.
 */
@Component
public class ConsistentHashRing {

    private static final Logger log = LoggerFactory.getLogger(ConsistentHashRing.class);

    private final ConcurrentSkipListMap<Long, String> ring = new ConcurrentSkipListMap<>();
    private final Map<String, Integer> nodeVirtualNodeCount = new HashMap<>();
    private int defaultVirtualNodes = 150;

    public void addNode(String nodeId, int virtualNodes) {
        nodeVirtualNodeCount.put(nodeId, virtualNodes);
        for (int i = 0; i < virtualNodes; i++) {
            long hash = hash(nodeId + "-vn-" + i);
            ring.put(hash, nodeId);
        }
        log.info("Added node '{}' to hash ring with {} virtual nodes. Total ring size: {}",
                nodeId, virtualNodes, ring.size());
    }

    public void addNode(String nodeId) {
        addNode(nodeId, defaultVirtualNodes);
    }

    public void removeNode(String nodeId) {
        Integer vNodes = nodeVirtualNodeCount.remove(nodeId);
        if (vNodes != null) {
            for (int i = 0; i < vNodes; i++) {
                long hash = hash(nodeId + "-vn-" + i);
                ring.remove(hash);
            }
            log.info("Removed node '{}' from hash ring. Total ring size: {}", nodeId, ring.size());
        }
    }

    public String getNode(String key) {
        if (ring.isEmpty()) {
            throw new IllegalStateException("Hash ring is empty — no cache nodes registered");
        }
        long hash = hash(key);
        Map.Entry<Long, String> entry = ring.ceilingEntry(hash);
        if (entry == null) {
            entry = ring.firstEntry();
        }
        return entry.getValue();
    }

    public DebugInfo getDebugInfo(String key) {
        long hash = hash(key);
        Map.Entry<Long, String> entry = ring.ceilingEntry(hash);
        if (entry == null) {
            entry = ring.firstEntry();
        }
        return new DebugInfo(key, Long.toHexString(hash), entry.getValue(), entry.getKey().intValue());
    }

    public Set<String> getNodeIds() {
        return Collections.unmodifiableSet(nodeVirtualNodeCount.keySet());
    }

    public int getRingSize() {
        return ring.size();
    }

    private long hash(String key) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(key.getBytes(StandardCharsets.UTF_8));
            long hash = 0;
            for (int i = 0; i < 8; i++) {
                hash = (hash << 8) | (digest[i] & 0xFF);
            }
            return hash;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 not available", e);
        }
    }

    public record DebugInfo(String key, String hashValue, String assignedNode, int ringPosition) {}
}
