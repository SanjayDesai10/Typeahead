package com.typeahead.service;

import com.typeahead.cache.CacheManager;
import com.typeahead.model.Query;
import com.typeahead.repository.QueryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class BatchWriterService {

    private static final Logger log = LoggerFactory.getLogger(BatchWriterService.class);

    private final QueryRepository queryRepository;
    private final CacheManager cacheManager;
    private final TrendingService trendingService;

    private final ConcurrentHashMap<String, AtomicLong> buffer = new ConcurrentHashMap<>();
    private final AtomicLong totalSearchesReceived = new AtomicLong(0);
    private final AtomicLong totalDbWrites = new AtomicLong(0);
    private final AtomicLong flushCount = new AtomicLong(0);
    private volatile String lastFlushAt = "never";

    private static final int MAX_BUFFER_SIZE = 100;

    public BatchWriterService(QueryRepository queryRepository,
                              CacheManager cacheManager,
                              TrendingService trendingService) {
        this.queryRepository = queryRepository;
        this.cacheManager = cacheManager;
        this.trendingService = trendingService;
    }

    public void enqueue(String queryText) {
        String normalized = queryText.trim().toLowerCase();
        if (normalized.isEmpty()) return;

        totalSearchesReceived.incrementAndGet();
        buffer.computeIfAbsent(normalized, k -> new AtomicLong(0)).incrementAndGet();
        trendingService.recordSearch(normalized);

        if (buffer.size() >= MAX_BUFFER_SIZE) {
            log.info("Buffer size {} exceeded threshold {}, triggering flush", buffer.size(), MAX_BUFFER_SIZE);
            flush();
        }
    }

    @Scheduled(fixedDelayString = "${typeahead.batch.flush-interval-ms:5000}")
    public void scheduledFlush() {
        if (!buffer.isEmpty()) {
            flush();
        }
    }

    @Transactional
    public synchronized void flush() {
        if (buffer.isEmpty()) return;

        Map<String, Long> snapshot = new HashMap<>();
        buffer.forEach((key, count) -> {
            long val = count.getAndSet(0);
            if (val > 0) {
                snapshot.put(key, val);
            }
        });
        buffer.entrySet().removeIf(e -> e.getValue().get() == 0);

        if (snapshot.isEmpty()) return;

        int totalSearches = snapshot.values().stream().mapToInt(Long::intValue).sum();

        for (Map.Entry<String, Long> entry : snapshot.entrySet()) {
            String queryText = entry.getKey();
            long additionalCount = entry.getValue();

            Query query = queryRepository.findByQueryTextIgnoreCase(queryText)
                    .orElse(Query.builder()
                            .queryText(queryText)
                            .count(0L)
                            .trendingScore(0.0)
                            .createdAt(LocalDateTime.now())
                            .build());

            query.setCount(query.getCount() + additionalCount);
            query.setLastSearchedAt(LocalDateTime.now());
            double trendingScore = trendingService.computeTrendingScore(queryText, query.getCount());
            query.setTrendingScore(trendingScore);
            queryRepository.save(query);
            cacheManager.invalidateForQuery(queryText);
        }

        totalDbWrites.incrementAndGet();
        flushCount.incrementAndGet();
        lastFlushAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        log.info("Batch flush: {} unique queries, {} total searches → 1 DB transaction (flush #{})",
                snapshot.size(), totalSearches, flushCount.get());
    }

    public long getTotalSearchesReceived() { return totalSearchesReceived.get(); }
    public long getTotalDbWrites() { return totalDbWrites.get(); }
    public double getWriteReductionRatio() {
        long writes = totalDbWrites.get();
        long searches = totalSearchesReceived.get();
        if (writes == 0) return 0;
        return (double) searches / writes;
    }
    public int getCurrentBufferSize() { return buffer.size(); }
    public long getFlushCount() { return flushCount.get(); }
    public String getLastFlushAt() { return lastFlushAt; }
}
