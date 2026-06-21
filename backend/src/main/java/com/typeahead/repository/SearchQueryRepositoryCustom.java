package com.typeahead.repository;

import java.time.Instant;

/**
 * Custom repository interface for search query upserts.
 * Permits database-specific implementations (e.g., PostgreSQL native ON CONFLICT vs. H2 MERGE).
 */
public interface SearchQueryRepositoryCustom {

    /**
     * Upsert a search query count in the database.
     *
     * @param query The query string.
     * @param count The count increment value.
     * @param now   The current timestamp.
     * @return Number of rows affected.
     */
    int upsertQueryCount(String query, long count, Instant now);
}
