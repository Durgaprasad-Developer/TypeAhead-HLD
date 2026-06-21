package com.typeahead.core.dataset;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Mutable, in-memory record for a single distinct query.
 *
 * <p>Per DESIGN.md §9.2, each query carries two separate counts:
 * <ul>
 *   <li>{@code overallCount} — all-time popularity (historical signal).</li>
 *   <li>{@code recentCount} — searches within the last {@code trending.window-days}
 *       (freshness signal, recomputed periodically by
 *       {@link com.typeahead.service.TrendingService}).</li>
 * </ul>
 *
 * <p>Trending score = 0.3 × overallCount + 0.7 × recentCount
 * (weights defined in DESIGN.md §12.1 and {@code application.yml}).
 *
 * <p>Thread safety: both counts use {@link AtomicLong} so that concurrent
 * increments from POST /search requests don't produce lost-update races without
 * needing coarse-grained locking.
 *
 * <p>Zero Spring annotations — pure Java, unit-testable with {@code new QueryRecord(...)}.
 */
public class QueryRecord {

    private final String    query;
    private final AtomicLong overallCount;
    private final AtomicLong recentCount;
    private volatile Instant lastSearched;

    /**
     * Full constructor — used by {@link DatasetLoader} when loading the
     * preprocessed CSV at startup.
     *
     * @param query        The normalised query string.
     * @param overallCount Initial all-time count (from the dataset).
     * @param recentCount  Initial recent count (0 for historical data; will be
     *                     updated by {@link com.typeahead.service.TrendingService}).
     * @param lastSearched Timestamp of the most recent known search.
     */
    public QueryRecord(String query, long overallCount, long recentCount, Instant lastSearched) {
        this.query        = query;
        this.overallCount = new AtomicLong(overallCount);
        this.recentCount  = new AtomicLong(recentCount);
        this.lastSearched = lastSearched;
    }

    /**
     * Convenience constructor for new queries submitted at runtime
     * (POST /search for a query not previously in the dataset).
     * Initial counts are both 1; lastSearched is now.
     */
    public QueryRecord(String query) {
        this(query, 1L, 1L, Instant.now());
    }

    // ── Mutation methods (called on search submission) ─────────────────────

    /**
     * Atomically increment the all-time count by 1.
     * Returns the new value.
     */
    public long incrementOverall() {
        return overallCount.incrementAndGet();
    }

    /**
     * Atomically increment the recent count by 1.
     * Returns the new value.
     */
    public long incrementRecent() {
        return recentCount.incrementAndGet();
    }

    /**
     * Set the recent count to an absolute value.
     * Called by {@link com.typeahead.service.TrendingService} during periodic
     * recompute (Option A: sliding window — count of timestamped events in last N days).
     */
    public void setRecentCount(long value) {
        recentCount.set(value);
    }

    /** Update the lastSearched timestamp to now. */
    public void updateTimestamp() {
        this.lastSearched = Instant.now();
    }

    // ── Accessors ──────────────────────────────────────────────────────────

    public String  getQuery()        { return query; }
    public long    getOverallCount() { return overallCount.get(); }
    public long    getRecentCount()  { return recentCount.get(); }
    public Instant getLastSearched() { return lastSearched; }

    @Override
    public String toString() {
        return "QueryRecord{query='" + query + '\'' +
               ", overallCount=" + overallCount.get() +
               ", recentCount=" + recentCount.get() + '}';
    }
}
