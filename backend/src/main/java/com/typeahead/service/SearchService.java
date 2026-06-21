package com.typeahead.service;

import com.typeahead.core.batch.BatchQueue;
import com.typeahead.core.cache.CacheNode;
import com.typeahead.core.cache.ConsistentHashRing;
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
 * <p>Enqueues updates to the {@link BatchQueue} for background persistence.
 *
 * <p>Per DESIGN.md §10.4, invalidates all query prefixes in the consistent hashing cache.
 */
@Service
public class SearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchService.class);

    private final QueryIndex         queryIndex;
    private final Trie               trie;
    private final BatchQueue         batchQueue;
    private final ConsistentHashRing consistentHashRing;

    public SearchService(QueryIndex queryIndex,
                         Trie trie,
                         BatchQueue batchQueue,
                         ConsistentHashRing consistentHashRing) {
        this.queryIndex         = queryIndex;
        this.trie               = trie;
        this.batchQueue         = batchQueue;
        this.consistentHashRing = consistentHashRing;
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

        // Enqueue update into BatchQueue for DB persistence
        batchQueue.enqueue(query);

        // Invalidate cache for all prefixes of this query
        for (int i = 1; i <= query.length(); i++) {
            String prefix = query.substring(0, i);
            CacheNode node = consistentHashRing.getNode(prefix);
            if (node != null) {
                node.invalidate(prefix);
                log.debug("SearchService: invalidated cache key '{}' on node '{}'", prefix, node.getId());
            }
        }
    }
}
