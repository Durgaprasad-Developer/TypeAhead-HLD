package com.typeahead.controller;

import com.typeahead.service.SuggestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

/**
 * Controller exposing the typeahead suggestion API.
 *
 * <p>Exposes:
 * <pre>
 *   GET /suggest?q={prefix}
 * </pre>
 *
 * <p>Per DESIGN.md §11, returns a JSON array of up to 10 strings. If the prefix
 * is empty, null, or has no matches, returns 200 OK with an empty array {@code []}.
 */
@RestController
@Tag(name = "Suggestion API", description = "Endpoints for retrieving typeahead suggestions")
public class SuggestionController {

    private final SuggestionService suggestionService;

    public SuggestionController(SuggestionService suggestionService) {
        this.suggestionService = suggestionService;
    }

    @GetMapping("/suggest")
    @Operation(summary = "Get typeahead suggestions", description = "Returns up to 10 suggestion strings for a prefix")
    public ResponseEntity<List<String>> suggest(@RequestParam(value = "q", required = false) String query) {
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        List<String> suggestions = suggestionService.getSuggestions(query);
        return ResponseEntity.ok(suggestions);
    }
}
