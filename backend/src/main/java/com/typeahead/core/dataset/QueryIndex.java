package com.typeahead.core.dataset;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory O(1) exact-match store for {@link QueryRecord}s.
 *
 * <p>Per DESIGN.md §9.4, this is a {@link ConcurrentHashMap} from the full,
 * normalised query string to its record. It exists alongside the Trie because
 * the Trie answers "what queries start with X?" in O(L) but cannot efficiently
 * answer "does this exact query exist? update its count" — that's an O(1)
 * HashMap operation. The viva answer for "why two structures?" is exactly this.
 *
 * <p>The Trie holds <em>references</em> to the same {@link QueryRecord} objects
 * stored here — never independent copies. A count update through {@code update()}
 * is immediately visible through the Trie pointer, so the two structures can
 * never diverge (DESIGN.md §9.3).
 *
 * <p>Thread safety: {@link ConcurrentHashMap} provides safe concurrent reads and
 * writes. The common case (POST /search for an existing query) only calls
 * {@link QueryRecord#incrementOverall()} and {@link QueryRecord#incrementRecent()},
 * which are themselves atomic ({@link java.util.concurrent.atomic.AtomicLong}).
 *
 * <p>Zero Spring annotations — pure Java.
 */
public class QueryIndex {

    private final ConcurrentHashMap<String, QueryRecord> store = new ConcurrentHashMap<>();

    /**
     * Look up a query by exact normalised string.
     *
     * @param query The normalised (lowercase, trimmed) query.
     * @return An {@link Optional} containing the record, or empty if not found.
     */
    public Optional<QueryRecord> find(String query) {
        return Optional.ofNullable(store.get(query));
    }

    /**
     * Insert a new {@link QueryRecord} into the index.
     * Replaces any existing record for the same query string.
     *
     * @param query  The normalised query string.
     * @param record The record to store.
     */
    public void insert(String query, QueryRecord record) {
        store.put(query, record);
    }

    /**
     * Increment {@code overallCount} and {@code recentCount} for an existing query
     * and update its {@code lastSearched} timestamp.
     *
     * <p>This is called on the hot path (POST /search), so it does the minimal
     * possible work: no locking (counts are AtomicLong), no copying.
     *
     * @param query The normalised query string.
     * @return The updated {@link QueryRecord}, or {@link Optional#empty()} if the
     *         query was not found (caller should insert it instead).
     */
    public Optional<QueryRecord> update(String query) {
        QueryRecord record = store.get(query);
        if (record == null) return Optional.empty();
        record.incrementOverall();
        record.incrementRecent();
        record.updateTimestamp();
        return Optional.of(record);
    }

    /**
     * @param query The normalised query string.
     * @return True if the query exists in the index.
     */
    public boolean exists(String query) {
        return store.containsKey(query);
    }

    /**
     * Returns all records in the index.
     * Used by {@link com.typeahead.service.TrendingService} to iterate over all
     * queries during the periodic recentCount recompute.
     */
    public Collection<QueryRecord> getAllRecords() {
        return store.values();
    }

    /** Returns the total number of unique queries loaded. */
    public int size() {
        return store.size();
    }
}
