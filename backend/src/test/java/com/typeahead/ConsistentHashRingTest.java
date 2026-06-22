package com.typeahead;

import com.typeahead.cache.ConsistentHashRing;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the ConsistentHashRing — the core distributed cache routing component.
 * Tests cover: determinism, distribution, node add/remove, and edge cases.
 */
class ConsistentHashRingTest {

    private ConsistentHashRing ring;

    @BeforeEach
    void setUp() {
        ring = new ConsistentHashRing();
    }

    @Test
    @DisplayName("getNode throws when ring is empty")
    void testEmptyRingThrows() {
        assertThrows(IllegalStateException.class, () -> ring.getNode("any-key"));
    }

    @Test
    @DisplayName("Single node always returns that node")
    void testSingleNode() {
        ring.addNode("redis-node-0");
        assertEquals("redis-node-0", ring.getNode("iphone"));
        assertEquals("redis-node-0", ring.getNode("samsung"));
        assertEquals("redis-node-0", ring.getNode("how to cook pasta"));
    }

    @Test
    @DisplayName("Same key always maps to the same node (deterministic)")
    void testDeterminism() {
        ring.addNode("redis-node-0");
        ring.addNode("redis-node-1");
        ring.addNode("redis-node-2");

        String firstResult = ring.getNode("iphone 15");
        for (int i = 0; i < 100; i++) {
            assertEquals(firstResult, ring.getNode("iphone 15"),
                    "Hash ring must be deterministic for the same key");
        }
    }

    @Test
    @DisplayName("Keys are distributed across all 3 nodes")
    void testDistributionAcrossNodes() {
        ring.addNode("redis-node-0", 150);
        ring.addNode("redis-node-1", 150);
        ring.addNode("redis-node-2", 150);

        Map<String, Integer> distribution = new HashMap<>();
        int totalKeys = 10000;

        for (int i = 0; i < totalKeys; i++) {
            String key = "prefix-" + i;
            String node = ring.getNode(key);
            distribution.merge(node, 1, Integer::sum);
        }

        // All 3 nodes should receive keys
        assertEquals(3, distribution.size(), "All 3 nodes should receive at least one key");

        // Each node should get at least 20% of keys (reasonable for 150 vnodes)
        for (Map.Entry<String, Integer> entry : distribution.entrySet()) {
            double ratio = (double) entry.getValue() / totalKeys;
            assertTrue(ratio > 0.20,
                    String.format("Node %s got only %.1f%% of keys — expected > 20%%",
                            entry.getKey(), ratio * 100));
            assertTrue(ratio < 0.45,
                    String.format("Node %s got %.1f%% of keys — expected < 45%% (uneven)",
                            entry.getKey(), ratio * 100));
        }
    }

    @Test
    @DisplayName("Ring size equals nodes × virtual nodes")
    void testRingSize() {
        ring.addNode("redis-node-0", 150);
        ring.addNode("redis-node-1", 150);
        ring.addNode("redis-node-2", 150);

        assertEquals(450, ring.getRingSize(),
                "3 nodes × 150 virtual nodes = 450 ring entries");
    }

    @Test
    @DisplayName("Removing a node redistributes keys to remaining nodes")
    void testNodeRemoval() {
        ring.addNode("redis-node-0", 150);
        ring.addNode("redis-node-1", 150);
        ring.addNode("redis-node-2", 150);

        // Record assignments before removal
        Map<String, String> beforeRemoval = new HashMap<>();
        for (int i = 0; i < 1000; i++) {
            String key = "query-" + i;
            beforeRemoval.put(key, ring.getNode(key));
        }

        // Remove node-2
        ring.removeNode("redis-node-2");
        assertEquals(300, ring.getRingSize(), "Ring should shrink to 300 entries");

        // Verify: keys that were on node-0 or node-1 should still map there
        int stayedSame = 0;
        int reassigned = 0;
        for (Map.Entry<String, String> entry : beforeRemoval.entrySet()) {
            String newNode = ring.getNode(entry.getKey());
            assertNotEquals("redis-node-2", newNode, "No key should map to removed node");
            if (newNode.equals(entry.getValue())) {
                stayedSame++;
            } else {
                reassigned++;
            }
        }

        // Most keys on surviving nodes should stay (consistent hashing property)
        long keysOnSurvivors = beforeRemoval.values().stream()
                .filter(n -> !n.equals("redis-node-2")).count();
        double stabilityRate = (double) stayedSame / keysOnSurvivors;
        assertTrue(stabilityRate > 0.90,
                String.format("Expected >90%% stability for surviving nodes, got %.1f%%", stabilityRate * 100));

        assertTrue(reassigned > 0, "Some keys should be reassigned from the removed node");
    }

    @Test
    @DisplayName("Adding a node only steals some keys from existing nodes")
    void testNodeAddition() {
        ring.addNode("redis-node-0", 150);
        ring.addNode("redis-node-1", 150);

        Map<String, String> before = new HashMap<>();
        for (int i = 0; i < 1000; i++) {
            String key = "test-key-" + i;
            before.put(key, ring.getNode(key));
        }

        // Add a third node
        ring.addNode("redis-node-2", 150);
        assertEquals(450, ring.getRingSize());

        int movedToNewNode = 0;
        int stayed = 0;
        for (Map.Entry<String, String> entry : before.entrySet()) {
            String newNode = ring.getNode(entry.getKey());
            if (newNode.equals("redis-node-2")) {
                movedToNewNode++;
            } else if (newNode.equals(entry.getValue())) {
                stayed++;
            }
        }

        // New node should steal roughly 1/3 of keys
        double movedRatio = (double) movedToNewNode / 1000;
        assertTrue(movedRatio > 0.20 && movedRatio < 0.45,
                String.format("New node stole %.1f%% of keys — expected ~33%%", movedRatio * 100));
    }

    @Test
    @DisplayName("Debug info provides correct hash and node assignment")
    void testDebugInfo() {
        ring.addNode("redis-node-0", 150);
        ring.addNode("redis-node-1", 150);

        ConsistentHashRing.DebugInfo info = ring.getDebugInfo("test-prefix");
        assertNotNull(info);
        assertNotNull(info.hashValue());
        assertTrue(info.assignedNode().startsWith("redis-node-"),
                "Assigned node should be a known node");
        assertEquals("test-prefix", info.key());
    }

    @Test
    @DisplayName("getNodeIds returns all registered node IDs")
    void testGetNodeIds() {
        ring.addNode("redis-node-0");
        ring.addNode("redis-node-1");
        ring.addNode("redis-node-2");

        assertEquals(3, ring.getNodeIds().size());
        assertTrue(ring.getNodeIds().contains("redis-node-0"));
        assertTrue(ring.getNodeIds().contains("redis-node-1"));
        assertTrue(ring.getNodeIds().contains("redis-node-2"));
    }

    @Test
    @DisplayName("Different prefixes of the same word map to potentially different nodes")
    void testPrefixVariation() {
        ring.addNode("redis-node-0", 150);
        ring.addNode("redis-node-1", 150);
        ring.addNode("redis-node-2", 150);

        // These are all different keys and should produce valid node assignments
        String node1 = ring.getNode("i");
        String node2 = ring.getNode("ip");
        String node3 = ring.getNode("iph");
        String node4 = ring.getNode("ipho");

        // Just verify they all return valid nodes
        for (String node : new String[]{node1, node2, node3, node4}) {
            assertTrue(node.startsWith("redis-node-"), "All prefixes should resolve to valid nodes");
        }
    }
}
