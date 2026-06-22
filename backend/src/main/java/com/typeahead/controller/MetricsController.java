package com.typeahead.controller;

import com.typeahead.cache.CacheManager;
import com.typeahead.dto.MetricsResponse;
import com.typeahead.repository.QueryRepository;
import com.typeahead.service.BatchWriterService;
import com.typeahead.service.SuggestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class MetricsController {

    private static final Logger log = LoggerFactory.getLogger(MetricsController.class);

    private final BatchWriterService batchWriterService;
    private final CacheManager cacheManager;
    private final SuggestionService suggestionService;
    private final QueryRepository queryRepository;

    public MetricsController(BatchWriterService batchWriterService,
                             CacheManager cacheManager,
                             SuggestionService suggestionService,
                             QueryRepository queryRepository) {
        this.batchWriterService = batchWriterService;
        this.cacheManager = cacheManager;
        this.suggestionService = suggestionService;
        this.queryRepository = queryRepository;
    }

    @GetMapping("/metrics")
    public ResponseEntity<MetricsResponse> getMetrics() {
        long totalHits = cacheManager.getTotalHits();
        long totalMisses = cacheManager.getTotalMisses();
        double hitRate = (totalHits + totalMisses) > 0
                ? (double) totalHits / (totalHits + totalMisses) : 0;

        MetricsResponse response = MetricsResponse.builder()
                .totalSearchesReceived(batchWriterService.getTotalSearchesReceived())
                .totalDbWrites(batchWriterService.getTotalDbWrites())
                .writeReductionRatio(Math.round(batchWriterService.getWriteReductionRatio() * 100.0) / 100.0)
                .currentBufferSize(batchWriterService.getCurrentBufferSize())
                .flushCount(batchWriterService.getFlushCount())
                .lastFlushAt(batchWriterService.getLastFlushAt())
                .totalCacheHits(totalHits)
                .totalCacheMisses(totalMisses)
                .overallHitRate(Math.round(hitRate * 1000.0) / 1000.0)
                .totalQueries(queryRepository.count())
                .totalDbReads(cacheManager.getTotalDbReads())
                .avgLatencyMs(Math.round(suggestionService.getAvgLatency() * 100.0) / 100.0)
                .p95LatencyMs(suggestionService.getP95Latency())
                .p99LatencyMs(suggestionService.getP99Latency())
                .build();

        return ResponseEntity.ok(response);
    }
}
