package com.typeahead.dto;

import java.util.List;

/**
 * Data Transfer Object representing a structured suggestions response.
 *
 * <p>Although the raw GET /suggest API returns a plain JSON array of strings
 * to match the API spec (DESIGN.md §11), this DTO is available to wrap the
 * results with metadata (e.g. the original query prefix) for internal routing
 * or if a structured JSON response is needed.
 */
public record SuggestionResponse(
    String prefix,
    List<String> suggestions
) {}
