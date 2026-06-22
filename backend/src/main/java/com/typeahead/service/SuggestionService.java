package com.typeahead.service;

import com.typeahead.cache.CacheManager;
import com.typeahead.dto.SuggestionResponse;
import com.typeahead.model.Query;
import com.typeahead.repository.QueryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Service
public class SuggestionService {

    private static final Logger log = LoggerFactory.getLogger(SuggestionService.class);

    private final QueryRepository queryRepository;
    private final CacheManager cacheManager;
    private final TrendingService trendingService;

    private final CopyOnWriteArrayList<Double> latencies = new CopyOnWriteArrayList<>();
    private static final int MAX_LATENCY_SAMPLES = 10000;

    public SuggestionService(QueryRepository queryRepository,
                             CacheManager cacheManager,
                             TrendingService trendingService) {
        this.queryRepository = queryRepository;
        this.cacheManager = cacheManager;
        this.trendingService = trendingService;
    }

    public SuggestionResponse getSuggestions(String prefix, boolean useTrending) {
        long startTime = System.nanoTime();

        if (prefix == null || prefix.trim().isEmpty()) {
            List<Query> topQueries = queryRepository.findTop10ByOrderByTrendingScoreDesc();
            List<SuggestionResponse.SuggestionItem> items = topQueries.stream()
                    .map(this::toSuggestionItem)
                    .collect(Collectors.toList());

            double latencyMs = (System.nanoTime() - startTime) / 1_000_000.0;
            recordLatency(latencyMs);

            return SuggestionResponse.builder()
                    .suggestions(items).cached(false).cacheNode("none")
                    .latencyMs(Math.round(latencyMs * 100.0) / 100.0).build();
        }

        String normalizedPrefix = prefix.trim().toLowerCase();

        CacheManager.CacheResult cacheResult = cacheManager.get(normalizedPrefix);

        if (cacheResult != null && cacheResult.hit()) {
            double latencyMs = (System.nanoTime() - startTime) / 1_000_000.0;
            recordLatency(latencyMs);
            return SuggestionResponse.builder()
                    .suggestions(cacheResult.items()).cached(true)
                    .cacheNode(cacheResult.nodeId())
                    .latencyMs(Math.round(latencyMs * 100.0) / 100.0).build();
        }

        cacheManager.incrementDbReads();
        List<Query> results;
        if (useTrending) {
            results = queryRepository.findTop10ByQueryTextStartingWithIgnoreCaseOrderByTrendingScoreDesc(normalizedPrefix);
        } else {
            results = queryRepository.findTop10ByQueryTextStartingWithIgnoreCaseOrderByCountDesc(normalizedPrefix);
        }

        List<SuggestionResponse.SuggestionItem> items = results.stream()
                .map(this::toSuggestionItem)
                .collect(Collectors.toList());

        String nodeId = cacheResult != null ? cacheResult.nodeId() : "unknown";
        cacheManager.put(normalizedPrefix, items);

        double latencyMs = (System.nanoTime() - startTime) / 1_000_000.0;
        recordLatency(latencyMs);

        return SuggestionResponse.builder()
                .suggestions(items).cached(false).cacheNode(nodeId)
                .latencyMs(Math.round(latencyMs * 100.0) / 100.0).build();
    }

    private SuggestionResponse.SuggestionItem toSuggestionItem(Query q) {
        return SuggestionResponse.SuggestionItem.builder()
                .query(q.getQueryText())
                .count(q.getCount())
                .trendingScore(Math.round(q.getTrendingScore() * 100.0) / 100.0)
                .build();
    }

    private void recordLatency(double latencyMs) {
        if (latencies.size() >= MAX_LATENCY_SAMPLES) {
            int half = MAX_LATENCY_SAMPLES / 2;
            List<Double> tail = new ArrayList<>(latencies.subList(half, latencies.size()));
            latencies.clear();
            latencies.addAll(tail);
        }
        latencies.add(latencyMs);
    }

    public double getAvgLatency() {
        if (latencies.isEmpty()) return 0;
        return latencies.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    public double getP95Latency() { return getPercentile(95); }
    public double getP99Latency() { return getPercentile(99); }

    private double getPercentile(int percentile) {
        if (latencies.isEmpty()) return 0;
        List<Double> sorted = new ArrayList<>(latencies);
        Collections.sort(sorted);
        int index = (int) Math.ceil(percentile / 100.0 * sorted.size()) - 1;
        return Math.round(sorted.get(Math.max(0, index)) * 100.0) / 100.0;
    }
}
