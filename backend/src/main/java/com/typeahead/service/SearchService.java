package com.typeahead.service;

import com.typeahead.core.dataset.QueryIndex;
import com.typeahead.core.dataset.QueryRecord;
import com.typeahead.core.trie.Trie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

/**
 * Service to handle search submissions and update search counts in memory.
 *
 * <p>Per DESIGN.md §9.4 / §13.1, this service updates the in-memory structures
 * ({@link QueryIndex} and {@link Trie}) immediately so subsequent suggestion requests
 * see the updated counts.
 *
 * <p>Later Milestones will:
 * <ul>
 *   <li>Enqueue updates into the BatchQueue for background database persistence (Milestone 9).</li>
 *   <li>Invalidate affected prefixes in the distributed cache layer (Milestone 11).</li>
 * </ul>
 */
@Service
public class SearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchService.class);

    private final QueryIndex queryIndex;
    private final Trie       trie;

    public SearchService(QueryIndex queryIndex, Trie trie) {
        this.queryIndex = queryIndex;
        this.trie       = trie;
    }

    /**
     * Handle search submission. Updates counts in-memory immediately.
     *
     * @param rawQuery The search query submitted by the user.
     * @throws IllegalArgumentException if the query is null or blank.
     */
    public void search(String rawQuery) {
        if (rawQuery == null || rawQuery.trim().isEmpty()) {
            throw new IllegalArgumentException("Search query cannot be empty");
        }

        String query = rawQuery.trim().toLowerCase();
        log.info("SearchService: search submitted for query '{}'", query);

        Optional<QueryRecord> existingRecord = queryIndex.find(query);
        if (existingRecord.isPresent()) {
            QueryRecord record = existingRecord.get();
            record.incrementOverall();
            record.incrementRecent();
            record.updateTimestamp();
            log.debug("SearchService: updated in-memory counts for existing query: {}", record);
        } else {
            // Create and insert a new QueryRecord with initial counts = 1
            QueryRecord record = new QueryRecord(query, 1L, 1L, Instant.now());
            queryIndex.insert(query, record);
            trie.insert(query, record); // Stores reference, not copy
            log.debug("SearchService: inserted new query record in-memory: {}", record);
        }

        // TODO Milestone 9: Enqueue update into BatchQueue for DB persistence
        // TODO Milestone 11: Invalidate cache for all prefixes of this query
    }
}
