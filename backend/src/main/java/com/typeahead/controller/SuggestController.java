package com.typeahead.controller;

import com.typeahead.dto.SuggestionResponse;
import com.typeahead.service.SuggestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class SuggestController {

    private static final Logger log = LoggerFactory.getLogger(SuggestController.class);
    private final SuggestionService suggestionService;

    public SuggestController(SuggestionService suggestionService) {
        this.suggestionService = suggestionService;
    }

    @GetMapping("/suggest")
    public ResponseEntity<SuggestionResponse> suggest(
            @RequestParam(value = "q", defaultValue = "") String prefix,
            @RequestParam(value = "trending", defaultValue = "true") boolean trending) {
        log.debug("Suggest request: prefix='{}', trending={}", prefix, trending);
        SuggestionResponse response = suggestionService.getSuggestions(prefix, trending);
        return ResponseEntity.ok(response);
    }
}
