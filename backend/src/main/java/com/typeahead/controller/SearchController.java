package com.typeahead.controller;

import com.typeahead.dto.SearchRequest;
import com.typeahead.service.SearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class SearchController {

    private static final Logger log = LoggerFactory.getLogger(SearchController.class);
    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @PostMapping("/search")
    public ResponseEntity<Map<String, String>> search(@RequestBody SearchRequest request) {
        log.debug("Search request: query='{}'", request.getQuery());
        Map<String, String> response = searchService.submitSearch(request.getQuery());
        return ResponseEntity.ok(response);
    }
}
