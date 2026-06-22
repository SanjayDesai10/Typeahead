package com.typeahead.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class SearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchService.class);
    private final BatchWriterService batchWriterService;

    public SearchService(BatchWriterService batchWriterService) {
        this.batchWriterService = batchWriterService;
    }

    public Map<String, String> submitSearch(String query) {
        if (query == null || query.trim().isEmpty()) {
            return Map.of("message", "Error", "error", "Query cannot be empty");
        }

        String normalized = query.trim().toLowerCase();
        log.info("Search submitted: '{}'", normalized);
        batchWriterService.enqueue(normalized);

        return Map.of("message", "Searched", "query", normalized);
    }
}
