package com.typeahead.core.trie;

import com.typeahead.core.dataset.QueryRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Trie} and {@link TrieNode}.
 *
 * <p>No Spring context is loaded — these are plain {@code new} instantiations,
 * proving the core classes are framework-agnostic (DESIGN.md §13.1 hard rule).
 */
class TrieTest {

    private Trie trie;

    @BeforeEach
    void setUp() {
        trie = new Trie();
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private QueryRecord rec(String query, long count) {
        return new QueryRecord(query, count, 0L, Instant.now());
    }

    // ── insert + findPrefix ──────────────────────────────────────────────────

    @Test
    void findPrefix_existingPrefix_returnsNode() {
        trie.insert("iphone", rec("iphone", 100));
        assertNotNull(trie.findPrefix("iph"), "prefix 'iph' should exist after inserting 'iphone'");
    }

    @Test
    void findPrefix_nonExistingPrefix_returnsNull() {
        trie.insert("iphone", rec("iphone", 100));
        assertNull(trie.findPrefix("xyz"), "non-existing prefix should return null");
    }

    @Test
    void findPrefix_emptyString_returnsRoot() {
        TrieNode root = trie.findPrefix("");
        assertNotNull(root, "empty prefix should return root node");
    }

    @Test
    void insert_queryWithSpaces_worksCorrectly() {
        // Real AOL queries contain spaces — fixed-26-array Tries break on these
        trie.insert("iphone 15", rec("iphone 15", 50));
        assertNotNull(trie.findPrefix("iphone 1"), "space-containing prefix should be found");
    }

    @Test
    void insert_queryWithDigits_worksCorrectly() {
        trie.insert("mp3 player", rec("mp3 player", 30));
        assertNotNull(trie.findPrefix("mp3"), "digit-containing prefix should be found");
    }

    @Test
    void insert_setsWordFlag() {
        QueryRecord qr = rec("hello", 10);
        trie.insert("hello", qr);
        TrieNode node = trie.findPrefix("hello");
        assertNotNull(node);
        assertTrue(node.isWord());
        assertEquals("hello", node.getQueryReference().getQuery());
    }

    // ── collectSuggestions ───────────────────────────────────────────────────

    @Test
    void collectSuggestions_prefixMatch_returnsAllDescendants() {
        trie.insert("iphone",         rec("iphone",         100));
        trie.insert("iphone 15",      rec("iphone 15",       80));
        trie.insert("iphone charger", rec("iphone charger",  60));
        trie.insert("ipad",           rec("ipad",            40));  // different prefix

        TrieNode startNode = trie.findPrefix("iphone");
        List<QueryRecord> results = trie.collectSuggestions(startNode, Integer.MAX_VALUE);

        assertEquals(3, results.size(), "should collect exactly 3 results under prefix 'iphone'");
        assertTrue(results.stream().anyMatch(r -> r.getQuery().equals("iphone")));
        assertTrue(results.stream().anyMatch(r -> r.getQuery().equals("iphone 15")));
        assertTrue(results.stream().anyMatch(r -> r.getQuery().equals("iphone charger")));
        assertFalse(results.stream().anyMatch(r -> r.getQuery().equals("ipad")),
                "'ipad' must NOT appear under prefix 'iphone'");
    }

    @Test
    void collectSuggestions_limitIsRespected() {
        for (int i = 0; i < 20; i++) {
            trie.insert("prefix" + i, rec("prefix" + i, i));
        }
        TrieNode startNode = trie.findPrefix("prefix");
        List<QueryRecord> results = trie.collectSuggestions(startNode, 10);
        assertTrue(results.size() <= 10, "collectSuggestions should not exceed the limit");
    }

    @Test
    void collectSuggestions_nullNode_returnsEmpty() {
        List<QueryRecord> results = trie.collectSuggestions(null, 10);
        assertTrue(results.isEmpty(), "null startNode should return empty list");
    }

    @Test
    void collectSuggestions_noMatchingPrefix_returnsEmpty() {
        trie.insert("google", rec("google", 100));
        TrieNode startNode = trie.findPrefix("xyz");
        List<QueryRecord> results = trie.collectSuggestions(startNode, 10);
        assertTrue(results.isEmpty());
    }

    // ── reference semantics ──────────────────────────────────────────────────

    @Test
    void trie_holdsReferenceNotCopy_seesMutatedCount() {
        // The Trie must store a reference to the same QueryRecord held in the
        // QueryIndex. If the count on the shared object changes, the Trie's
        // reference should reflect it without a re-insert.
        QueryRecord shared = rec("java", 100);
        trie.insert("java", shared);

        shared.incrementOverall();  // simulate a search submission update

        TrieNode node = trie.findPrefix("java");
        assertNotNull(node);
        assertEquals(101L, node.getQueryReference().getOverallCount(),
                "Trie reference should reflect the mutated count — not a stale copy");
    }
}
