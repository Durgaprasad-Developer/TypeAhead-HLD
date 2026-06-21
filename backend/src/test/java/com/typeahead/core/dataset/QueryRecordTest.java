package com.typeahead.core.dataset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link QueryRecord}.
 * Plain Java instantiation — no Spring context.
 */
class QueryRecordTest {

    private QueryRecord record;

    @BeforeEach
    void setUp() {
        record = new QueryRecord("iphone", 100L, 10L, Instant.now());
    }

    @Test
    void incrementOverall_returnsIncrementedValue() {
        long result = record.incrementOverall();
        assertEquals(101L, result);
        assertEquals(101L, record.getOverallCount());
    }

    @Test
    void incrementRecent_returnsIncrementedValue() {
        long result = record.incrementRecent();
        assertEquals(11L, result);
        assertEquals(11L, record.getRecentCount());
    }

    @Test
    void setRecentCount_updatesValue() {
        record.setRecentCount(999L);
        assertEquals(999L, record.getRecentCount());
    }

    @Test
    void updateTimestamp_changesLastSearched() throws InterruptedException {
        Instant before = record.getLastSearched();
        Thread.sleep(2); // ensure at least 1ms gap
        record.updateTimestamp();
        assertTrue(record.getLastSearched().isAfter(before),
                "lastSearched should advance after updateTimestamp()");
    }

    @Test
    void convenienceConstructor_setsCountsToOne() {
        QueryRecord newQuery = new QueryRecord("brand new query");
        assertEquals(1L, newQuery.getOverallCount());
        assertEquals(1L, newQuery.getRecentCount());
        assertNotNull(newQuery.getLastSearched());
    }

    @Test
    void concurrentIncrements_noLostUpdates() throws InterruptedException {
        // AtomicLong must handle 100 concurrent increments without lost updates
        QueryRecord shared = new QueryRecord("concurrent", 0L, 0L, Instant.now());
        int threads = 100;
        Thread[] pool = new Thread[threads];
        for (int i = 0; i < threads; i++) {
            pool[i] = new Thread(shared::incrementOverall);
            pool[i].start();
        }
        for (Thread t : pool) t.join();
        assertEquals(100L, shared.getOverallCount(),
                "All 100 concurrent increments must be counted — no lost updates");
    }
}
