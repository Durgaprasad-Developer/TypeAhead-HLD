package com.typeahead.repository;

import com.typeahead.entity.SearchQuery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * JPA Repository for {@link SearchQuery} entity.
 *
 * <p>Per DESIGN.md §13.2, queries database records by the exact unique query string.
 */
@Repository
public interface SearchQueryRepository extends JpaRepository<SearchQuery, Long>, SearchQueryRepositoryCustom {

    /**
     * Look up a query record by its exact query string.
     *
     * @param query The normalized query string.
     * @return An Optional containing the matching SearchQuery, or empty.
     */
    Optional<SearchQuery> findByQuery(String query);
}
