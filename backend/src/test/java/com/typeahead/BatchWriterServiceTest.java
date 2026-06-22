package com.typeahead;

import com.typeahead.model.Query;
import com.typeahead.repository.QueryRepository;
import com.typeahead.service.BatchWriterService;
import com.typeahead.service.TrendingService;
import com.typeahead.cache.CacheManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BatchWriterService — the write-pressure reducer.
 * Uses a stub CacheManager and TrendingService since Mockito on Java 25
 * cannot mock concrete classes with Spring-managed constructor dependencies.
 */
@ExtendWith(MockitoExtension.class)
class BatchWriterServiceTest {

    @Mock
    private QueryRepository queryRepository;

    // Stub implementations to bypass Mockito's Java 25 limitations
    // for classes with complex constructors (CacheManager depends on ConsistentHashRing, Redis, etc.)
    private StubCacheManager stubCacheManager;
    private StubTrendingService stubTrendingService;

    private BatchWriterService batchWriter;

    /** Minimal stub that replaces CacheManager without needing Redis/HashRing */
    static class StubCacheManager extends CacheManager {
        int invalidateCount = 0;
        String lastInvalidatedQuery = null;

        StubCacheManager() {
            // Pass nulls — we never call any real methods
            super(null, new java.util.HashMap<>(), null);
        }

        @Override
        public void invalidateForQuery(String query) {
            invalidateCount++;
            lastInvalidatedQuery = query;
        }

        // Override init to prevent NPE on @PostConstruct
        @Override
        public void init() { /* no-op */ }
    }

    /** Minimal stub that replaces TrendingService */
    static class StubTrendingService extends TrendingService {
        int recordCount = 0;
        String lastRecordedQuery = null;

        StubTrendingService() {
            super(null, null);
        }

        @Override
        public void recordSearch(String queryText) {
            recordCount++;
            lastRecordedQuery = queryText;
        }
    }

    @BeforeEach
    void setUp() {
        stubCacheManager = new StubCacheManager();
        stubTrendingService = new StubTrendingService();
        batchWriter = new BatchWriterService(queryRepository, stubCacheManager, stubTrendingService);
    }

    @Test
    @DisplayName("Enqueue increments totalSearchesReceived counter")
    void testEnqueueCountsSearches() {
        batchWriter.enqueue("iphone 15");
        batchWriter.enqueue("samsung galaxy");
        batchWriter.enqueue("iphone 15");

        assertEquals(3, batchWriter.getTotalSearchesReceived(),
                "All 3 enqueue calls should be counted");
    }

    @Test
    @DisplayName("Flush aggregates duplicate queries into a single DB write per unique query")
    void testFlushAggregation() {
        when(queryRepository.findByQueryTextIgnoreCase(anyString()))
                .thenReturn(Optional.empty());
        when(queryRepository.save(any(Query.class))).thenAnswer(i -> i.getArgument(0));

        // Enqueue the same query 5 times
        for (int i = 0; i < 5; i++) {
            batchWriter.enqueue("macbook pro");
        }

        // Manually trigger flush
        batchWriter.flush();

        // Should result in exactly 1 save call (aggregated), not 5
        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
        verify(queryRepository, times(1)).save(captor.capture());

        Query saved = captor.getValue();
        assertEquals("macbook pro", saved.getQueryText());
        assertEquals(5L, saved.getCount(), "Count should be 5 (aggregated)");
    }

    @Test
    @DisplayName("Flush updates existing query count rather than creating new entry")
    void testFlushUpdatesExisting() {
        Query existing = Query.builder()
                .queryText("laptop")
                .count(100L)
                .trendingScore(50.0)
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();

        when(queryRepository.findByQueryTextIgnoreCase("laptop"))
                .thenReturn(Optional.of(existing));
        when(queryRepository.save(any(Query.class))).thenAnswer(i -> i.getArgument(0));

        batchWriter.enqueue("laptop");
        batchWriter.enqueue("laptop");
        batchWriter.enqueue("laptop");
        batchWriter.flush();

        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
        verify(queryRepository).save(captor.capture());

        assertEquals(103L, captor.getValue().getCount(),
                "Should add 3 to existing count of 100");
    }

    @Test
    @DisplayName("Empty flush is a no-op — no DB writes")
    void testEmptyFlush() {
        batchWriter.flush();
        verify(queryRepository, never()).save(any());
        assertEquals(0, batchWriter.getFlushCount());
    }

    @Test
    @DisplayName("Flush invalidates cache entries for each flushed query")
    void testFlushInvalidatesCache() {
        when(queryRepository.findByQueryTextIgnoreCase(anyString()))
                .thenReturn(Optional.empty());
        when(queryRepository.save(any(Query.class))).thenAnswer(i -> i.getArgument(0));

        batchWriter.enqueue("pixel 8 pro");
        batchWriter.flush();

        assertEquals(1, stubCacheManager.invalidateCount);
        assertEquals("pixel 8 pro", stubCacheManager.lastInvalidatedQuery);
    }

    @Test
    @DisplayName("Flush records trending activity for each search")
    void testFlushRecordsTrending() {
        batchWriter.enqueue("trending search");
        batchWriter.enqueue("trending search");

        assertEquals(2, stubTrendingService.recordCount);
        assertEquals("trending search", stubTrendingService.lastRecordedQuery);
    }

    @Test
    @DisplayName("Write reduction ratio is correctly calculated")
    void testWriteReductionRatio() {
        when(queryRepository.findByQueryTextIgnoreCase(anyString()))
                .thenReturn(Optional.empty());
        when(queryRepository.save(any(Query.class))).thenAnswer(i -> i.getArgument(0));

        // 10 searches → 1 flush → ratio = 10:1
        for (int i = 0; i < 10; i++) {
            batchWriter.enqueue("same query");
        }
        batchWriter.flush();

        assertEquals(10, batchWriter.getTotalSearchesReceived());
        assertEquals(1, batchWriter.getTotalDbWrites());
        assertEquals(10.0, batchWriter.getWriteReductionRatio(), 0.01,
                "10 searches / 1 DB write = 10:1 reduction");
    }

    @Test
    @DisplayName("Normalizes queries to lowercase and trims whitespace")
    void testQueryNormalization() {
        when(queryRepository.findByQueryTextIgnoreCase(anyString()))
                .thenReturn(Optional.empty());
        when(queryRepository.save(any(Query.class))).thenAnswer(i -> i.getArgument(0));

        batchWriter.enqueue("  iPhone 15  ");
        batchWriter.enqueue("IPHONE 15");
        batchWriter.enqueue("iphone 15");
        batchWriter.flush();

        // All 3 should normalize to "iphone 15" → 1 save with count=3
        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
        verify(queryRepository, times(1)).save(captor.capture());
        assertEquals("iphone 15", captor.getValue().getQueryText());
        assertEquals(3L, captor.getValue().getCount());
    }

    @Test
    @DisplayName("Empty and blank queries are ignored")
    void testIgnoresEmptyQueries() {
        batchWriter.enqueue("");
        batchWriter.enqueue("   ");

        assertEquals(0, batchWriter.getTotalSearchesReceived(),
                "Empty/blank queries should not be counted");
        assertEquals(0, batchWriter.getCurrentBufferSize());
    }

    @Test
    @DisplayName("Concurrent enqueues are thread-safe")
    void testConcurrentEnqueues() throws InterruptedException {
        int threads = 8;
        int enqueuesPerThread = 500;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);

        for (int t = 0; t < threads; t++) {
            executor.submit(() -> {
                try {
                    for (int i = 0; i < enqueuesPerThread; i++) {
                        batchWriter.enqueue("concurrent-query");
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS), "All threads should complete");
        executor.shutdown();

        assertEquals(threads * enqueuesPerThread, batchWriter.getTotalSearchesReceived(),
                "All concurrent enqueues should be counted");
    }

    @Test
    @DisplayName("Flush count and lastFlushAt are updated after flush")
    void testFlushMetadata() {
        when(queryRepository.findByQueryTextIgnoreCase(anyString()))
                .thenReturn(Optional.empty());
        when(queryRepository.save(any(Query.class))).thenAnswer(i -> i.getArgument(0));

        assertEquals("never", batchWriter.getLastFlushAt());
        assertEquals(0, batchWriter.getFlushCount());

        batchWriter.enqueue("test query");
        batchWriter.flush();

        assertEquals(1, batchWriter.getFlushCount());
        assertNotEquals("never", batchWriter.getLastFlushAt(),
                "lastFlushAt should be updated after a flush");
    }

    @Test
    @DisplayName("scheduledFlush is a no-op when buffer is empty")
    void testScheduledFlushNoOp() {
        batchWriter.scheduledFlush();
        verify(queryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Multiple different queries result in multiple saves")
    void testMultipleDifferentQueries() {
        when(queryRepository.findByQueryTextIgnoreCase(anyString()))
                .thenReturn(Optional.empty());
        when(queryRepository.save(any(Query.class))).thenAnswer(i -> i.getArgument(0));

        batchWriter.enqueue("query alpha");
        batchWriter.enqueue("query beta");
        batchWriter.enqueue("query gamma");
        batchWriter.flush();

        // 3 unique queries → 3 save calls
        verify(queryRepository, times(3)).save(any(Query.class));
    }
}
