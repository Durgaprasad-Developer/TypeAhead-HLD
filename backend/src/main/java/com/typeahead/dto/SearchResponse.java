package com.typeahead.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Data Transfer Object representing the response to a search submission.
 *
 * <p>Per DESIGN.md §11, the response format is:
 * <pre>
 *   { "message": "Searched" }
 * </pre>
 */
public record SearchResponse(
    @Schema(description = "Confirmation message", example = "Searched")
    String message
) {}
