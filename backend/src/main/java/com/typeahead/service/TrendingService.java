package com.typeahead.service;

import com.typeahead.core.cache.CacheNode;
import com.typeahead.core.cache.ConsistentHashRing;
import com.typeahead.core.dataset.QueryIndex;
import com.typeahead.core.dataset.QueryRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

/**
 * Service to manage rolling-window trending scores (Option A: Sliding Window).
 *
 * <p>Per DESIGN.md §12.2, maintains an in-memory buffer of timestamped search events
 * submitted at runtime. Periodically evicts expired events (older than window-days)
 * and updates each {@link QueryRecord}'s recent count.
 */
@Service
public class TrendingService {

    private static final Logger log = LoggerFactory.getLogger(TrendingService.class);

    private final QueryIndex         queryIndex;
    private final ConsistentHashRing consistentHashRing;
    private final int                windowDays;

    // Buffer for active search events
    private final ConcurrentLinkedQueue<SearchEvent> searchEvents;

    public TrendingService(QueryIndex queryIndex,
                           ConsistentHashRing consistentHashRing,
                           @Value("${typeahead.trending.window-days:7}") int windowDays) {
        this.queryIndex         = queryIndex;
        this.consistentHashRing = consistentHashRing;
        this.windowDays         = windowDays;
        this.searchEvents       = new ConcurrentLinkedQueue<>();
    }

    /** Record a search event at the current timestamp. */
    public void registerSearchEvent(String query) {
        if (query != null && !query.trim().isEmpty()) {
            searchEvents.add(new SearchEvent(query.trim().toLowerCase(), Instant.now()));
        }
    }

    /**
     * Periodically recomputes recent counts.
     * Scheduled using recompute-interval-minutes config.
     */
    @Scheduled(fixedDelayString = "#{T(java.lang.Integer).parseInt('${typeahead.trending.recompute-interval-minutes:5}') * 60 * 1000}")
    public void recomputeTrendingScores() {
        log.info("TrendingService: starting trending score recomputation. Active event store size={}", searchEvents.size());

        Instant cutoff = Instant.now().minus(Duration.ofDays(windowDays));
        int evictedCount = 0;

        // 1. Evict expired events (older than cutoff)
        SearchEvent head;
        while ((head = searchEvents.peek()) != null && head.getTimestamp().isBefore(cutoff)) {
            searchEvents.poll();
            evictedCount++;
        }

        if (evictedCount > 0) {
            log.info("TrendingService: evicted {} expired events older than {} days", evictedCount, windowDays);
        }

        // 2. Group active events by query and count them
        Map<String, Long> recentCounts = searchEvents.stream()
            .collect(Collectors.groupingBy(SearchEvent::getQuery, Collectors.counting()));

        log.info("TrendingService: grouped active events into {} distinct trending queries", recentCounts.size());

        // 3. Update recentCount for all records in the QueryIndex
        // For queries present in the active event store, set their recentCount.
        // For queries NOT present, set recentCount to 0.
        int updatedCount = 0;
        for (QueryRecord record : queryIndex.getAllRecords()) {
            Long newRecentCount = recentCounts.getOrDefault(record.getQuery(), 0L);
            long oldRecentCount = record.getRecentCount();

            if (oldRecentCount != newRecentCount) {
                record.setRecentCount(newRecentCount);
                updatedCount++;
            }
        }

        log.info("TrendingService: updated recent counts for {} QueryRecords", updatedCount);

        // 4. Invalidate all caches to force re-ranking on next suggests
        List<CacheNode> nodes = new java.util.ArrayList<>(consistentHashRing.getPhysicalNodes());
        for (CacheNode node : nodes) {
            node.clear();
        }
        log.info("TrendingService: flushed all cache nodes to prevent stale suggestions post-recomputation");
    }

    /** Returns active search events size (for testing/diagnostics). */
    public int getEventStoreSize() {
        return searchEvents.size();
    }

    /** Direct access to list for testing. */
    public ConcurrentLinkedQueue<SearchEvent> getSearchEvents() {
        return searchEvents;
    }

    /** Represents a single search event with timestamp. */
    public static class SearchEvent {
        private final String  query;
        private final Instant timestamp;

        public SearchEvent(String query, Instant timestamp) {
            this.query     = query;
            this.timestamp = timestamp;
        }

        public String getQuery()        { return query; }
        public Instant getTimestamp() { return timestamp; }
    }
}
