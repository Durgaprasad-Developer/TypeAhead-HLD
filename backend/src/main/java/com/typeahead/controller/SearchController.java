package com.typeahead.controller;

import com.typeahead.dto.SearchRequest;
import com.typeahead.dto.SearchResponse;
import com.typeahead.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller exposing the search submission API.
 *
 * <p>Exposes:
 * <pre>
 *   POST /search
 * </pre>
 *
 * <p>Per DESIGN.md §11, accepts a JSON body with {@code query}. If the query is empty
 * or null, returns a 400 Bad Request. Otherwise, increments the query counts in memory
 * and returns {@code {"message": "Searched"}}.
 */
@RestController
@Tag(name = "Search API", description = "Endpoints for submitting search queries")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @PostMapping("/search")
    @Operation(summary = "Submit a search query", description = "Increments query search counts in memory and enqueues for durability")
    public ResponseEntity<SearchResponse> search(@RequestBody SearchRequest request) {
        if (request == null || request.getQuery() == null || request.getQuery().trim().isEmpty()) {
            throw new IllegalArgumentException("Search query cannot be empty");
        }

        searchService.search(request.getQuery());
        return ResponseEntity.ok(new SearchResponse("Searched"));
    }
}
