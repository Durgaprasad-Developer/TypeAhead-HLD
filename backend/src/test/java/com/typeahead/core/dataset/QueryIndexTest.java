package com.typeahead.core.dataset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link QueryIndex}.
 * Plain Java instantiation — no Spring context.
 */
class QueryIndexTest {

    private QueryIndex index;

    @BeforeEach
    void setUp() {
        index = new QueryIndex();
    }

    private QueryRecord rec(String query, long count) {
        return new QueryRecord(query, count, 0L, Instant.now());
    }

    // ── find ─────────────────────────────────────────────────────────────────

    @Test
    void find_existingQuery_returnsRecord() {
        QueryRecord qr = rec("google", 1000);
        index.insert("google", qr);

        Optional<QueryRecord> result = index.find("google");
        assertTrue(result.isPresent());
        assertEquals("google", result.get().getQuery());
        assertEquals(1000L, result.get().getOverallCount());
    }

    @Test
    void find_nonExistingQuery_returnsEmpty() {
        Optional<QueryRecord> result = index.find("nothere");
        assertFalse(result.isPresent());
    }

    // ── insert ───────────────────────────────────────────────────────────────

    @Test
    void insert_thenFind_returnsInserted() {
        QueryRecord qr = rec("java tutorial", 500);
        index.insert("java tutorial", qr);
        assertTrue(index.find("java tutorial").isPresent());
    }

    @Test
    void insert_overwritesExisting() {
        index.insert("ebay", rec("ebay", 100));
        QueryRecord newRecord = rec("ebay", 999);
        index.insert("ebay", newRecord);
        assertEquals(999L, index.find("ebay").get().getOverallCount());
    }

    // ── update ───────────────────────────────────────────────────────────────

    @Test
    void update_existingQuery_incrementsBothCounts() {
        index.insert("iphone", rec("iphone", 10));
        Optional<QueryRecord> updated = index.update("iphone");
        assertTrue(updated.isPresent());
        assertEquals(11L, updated.get().getOverallCount());
        assertEquals(1L,  updated.get().getRecentCount());  // was 0, now 1
    }

    @Test
    void update_nonExistingQuery_returnsEmpty() {
        Optional<QueryRecord> result = index.update("unknownquery");
        assertFalse(result.isPresent(), "update on missing query must return empty, not throw");
    }

    // ── exists ───────────────────────────────────────────────────────────────

    @Test
    void exists_afterInsert_returnsTrue() {
        index.insert("yahoo", rec("yahoo", 5));
        assertTrue(index.exists("yahoo"));
    }

    @Test
    void exists_beforeInsert_returnsFalse() {
        assertFalse(index.exists("nothing"));
    }

    // ── size ─────────────────────────────────────────────────────────────────

    @Test
    void size_reflectsInsertedCount() {
        assertEquals(0, index.size());
        index.insert("a", rec("a", 1));
        index.insert("b", rec("b", 2));
        index.insert("c", rec("c", 3));
        assertEquals(3, index.size());
    }

    // ── O(1) reference sharing with Trie ─────────────────────────────────────

    @Test
    void update_mutatesSharedReference_visibleToCaller() {
        QueryRecord original = rec("shared", 50);
        index.insert("shared", original);

        // Simulate Trie holding a reference to the same object
        QueryRecord trieSideReference = index.find("shared").get();

        // Update via the index (as SearchService would)
        index.update("shared");

        // The Trie's reference should now see the incremented count,
        // because it's the same object — no copy was made
        assertEquals(51L, trieSideReference.getOverallCount(),
                "Trie-side reference must see count increment without re-insert");
    }
}
