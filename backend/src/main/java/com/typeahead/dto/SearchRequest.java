package com.typeahead.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Data Transfer Object representing a search submission request.
 *
 * <p>Per DESIGN.md §11, the request body format is:
 * <pre>
 *   { "query": "iphone" }
 * </pre>
 */
public class SearchRequest {

    @Schema(description = "The query string being searched", example = "iphone", requiredMode = Schema.RequiredMode.REQUIRED)
    private String query;

    public SearchRequest() {}

    public SearchRequest(String query) {
        this.query = query;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }
}
