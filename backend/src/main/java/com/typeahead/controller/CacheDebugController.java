package com.typeahead.controller;

import com.typeahead.cache.CacheManager;
import com.typeahead.dto.CacheDebugResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class CacheDebugController {

    private static final Logger log = LoggerFactory.getLogger(CacheDebugController.class);
    private final CacheManager cacheManager;

    public CacheDebugController(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @GetMapping("/cache/debug")
    public ResponseEntity<CacheDebugResponse> debugCache(
            @RequestParam(value = "prefix", defaultValue = "") String prefix) {
        log.debug("Cache debug request: prefix='{}'", prefix);
        CacheDebugResponse response = cacheManager.getDebugInfo(prefix);
        return ResponseEntity.ok(response);
    }
}
