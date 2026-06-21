package com.typeahead.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * JPA Entity representing the persisted search query counts in the database.
 *
 * <p>Per DESIGN.md §13.2:
 * <ul>
 *   <li>Table name: {@code search_queries}</li>
 *   <li>Primary key: {@code id} (BIGSERIAL)</li>
 *   <li>Unique query string: {@code query} (VARCHAR, unique, not null)</li>
 *   <li>Historical count: {@code overall_count} (BIGINT)</li>
 *   <li>Rolling window count: {@code recent_count} (BIGINT)</li>
 *   <li>Last searched: {@code last_searched} (TIMESTAMP)</li>
 *   <li>Created at: {@code created_at} (TIMESTAMP, set on insert)</li>
 *   <li>Updated at: {@code updated_at} (TIMESTAMP, updated on modification)</li>
 * </ul>
 */
@Entity
@Table(name = "search_queries", indexes = {
    @Index(name = "idx_search_queries_query", columnList = "query", unique = true)
})
public class SearchQuery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String query;

    @Column(name = "overall_count", nullable = false)
    private Long overallCount = 0L;

    @Column(name = "recent_count", nullable = false)
    private Long recentCount = 0L;

    @Column(name = "last_searched")
    private Instant lastSearched;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public SearchQuery() {}

    public SearchQuery(String query, Long overallCount, Long recentCount, Instant lastSearched) {
        this.query = query;
        this.overallCount = overallCount;
        this.recentCount = recentCount;
        this.lastSearched = lastSearched;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    // ── Getters and Setters ──────────────────────────────────────────────────

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public Long getOverallCount() {
        return overallCount;
    }

    public void setOverallCount(Long overallCount) {
        this.overallCount = overallCount;
    }

    public Long getRecentCount() {
        return recentCount;
    }

    public void setRecentCount(Long recentCount) {
        this.recentCount = recentCount;
    }

    public Instant getLastSearched() {
        return lastSearched;
    }

    public void setLastSearched(Instant lastSearched) {
        this.lastSearched = lastSearched;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "SearchQuery{" +
                "id=" + id +
                ", query='" + query + '\'' +
                ", overallCount=" + overallCount +
                ", recentCount=" + recentCount +
                ", lastSearched=" + lastSearched +
                '}';
    }
}
