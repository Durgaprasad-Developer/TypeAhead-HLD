package com.typeahead.service;

import com.typeahead.core.dataset.QueryIndex;
import com.typeahead.core.dataset.QueryRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class TrendingServiceIntegrationTest {

    @Autowired
    private SearchService searchService;

    @Autowired
    private TrendingService trendingService;

    @Autowired
    private QueryIndex queryIndex;

    @BeforeEach
    void setUp() {
        // Clear active event store
        trendingService.getSearchEvents().clear();
        
        // Reset query records' recent counts
        for (QueryRecord record : queryIndex.getAllRecords()) {
            record.setRecentCount(0L);
        }
    }

    @Test
    void testTrendingSlidingWindowRecomputationAndEviction() {
        String queryA = "trending-query-a";
        String queryB = "trending-query-b";

        // Insert initial QueryRecord shells in-memory so they exist in index
        queryIndex.insert(queryA, new QueryRecord(queryA, 10L, 0L, Instant.now()));
        queryIndex.insert(queryB, new QueryRecord(queryB, 5L, 0L, Instant.now()));

        // 1. Manually inject expired search events FIRST (so they reside at the head of the FIFO queue)
        Instant tenDaysAgo = Instant.now().minus(Duration.ofDays(10));
        trendingService.getSearchEvents().add(new TrendingService.SearchEvent(queryA, tenDaysAgo));
        trendingService.getSearchEvents().add(new TrendingService.SearchEvent(queryA, tenDaysAgo));

        // 2. Submit new search events via SearchService (appended after expired events)
        searchService.search(queryA);
        searchService.search(queryA);
        searchService.search(queryA); // x3 new searches for A
        searchService.search(queryB); // x1 new search for B

        // Total events in store = 2 (expired) + 4 (new) = 6
        assertEquals(6, trendingService.getEventStoreSize());

        // 3. Trigger trending recompute (should evict the 2 expired events from head)
        trendingService.recomputeTrendingScores();

        // Total active events should be 4 after eviction
        assertEquals(4, trendingService.getEventStoreSize());

        // 4. Verify counts updated (new events only)
        Optional<QueryRecord> recordA = queryIndex.find(queryA);
        assertTrue(recordA.isPresent());
        assertEquals(3L, recordA.get().getRecentCount());

        Optional<QueryRecord> recordB = queryIndex.find(queryB);
        assertTrue(recordB.isPresent());
        assertEquals(1L, recordB.get().getRecentCount());
    }
}
