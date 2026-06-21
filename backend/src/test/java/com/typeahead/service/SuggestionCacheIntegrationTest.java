package com.typeahead.service;

import com.typeahead.core.cache.CacheNode;
import com.typeahead.core.cache.ConsistentHashRing;
import com.typeahead.core.dataset.QueryIndex;
import com.typeahead.core.dataset.QueryRecord;
import com.typeahead.core.trie.Trie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class SuggestionCacheIntegrationTest {

    @Autowired
    private SuggestionService suggestionService;

    @Autowired
    private ConsistentHashRing consistentHashRing;

    @Autowired
    private Trie trie;

    @Autowired
    private QueryIndex queryIndex;

    @BeforeEach
    void setUp() {
        // Clear all caches
        for (CacheNode node : consistentHashRing.getPhysicalNodes()) {
            node.clear();
        }
        // Insert sample records in-memory
        QueryRecord r1 = new QueryRecord("cachetest-cat", 10L, 5L, Instant.now());
        QueryRecord r2 = new QueryRecord("cachetest-dog", 20L, 8L, Instant.now());
        queryIndex.insert("cachetest-cat", r1);
        queryIndex.insert("cachetest-dog", r2);
        trie.insert("cachetest-cat", r1);
        trie.insert("cachetest-dog", r2);
    }

    @Test
    void testConsistentHashingCacheRoutingAndIsolation() {
        String prefix = "cachetest";

        // 1. Initial request (should cause MISS and cache the result)
        List<String> results1 = suggestionService.getSuggestions(prefix);
        assertEquals(2, results1.size());
        assertTrue(results1.contains("cachetest-cat"));
        assertTrue(results1.contains("cachetest-dog"));

        // 2. Find which node the prefix was routed to
        CacheNode routedNode = consistentHashRing.getNode(prefix);
        assertNotNull(routedNode);

        // Verify the routed node contains the cache entry
        assertTrue(routedNode.getSnapshot().containsKey(prefix));
        assertEquals(results1, routedNode.get(prefix));

        // 3. Verify ISOLATION: No other physical cache node should store this prefix!
        int otherNodesWithKeyCount = 0;
        for (CacheNode node : consistentHashRing.getPhysicalNodes()) {
            if (!node.getId().equals(routedNode.getId())) {
                if (node.getSnapshot().containsKey(prefix)) {
                    otherNodesWithKeyCount++;
                }
            }
        }
        assertEquals(0, otherNodesWithKeyCount, "Consistent hashing violation: prefix key cached in multiple shards!");

        // 4. Modify the Trie record directly (bypass services) to see if next call hits cache
        QueryRecord r1 = queryIndex.find("cachetest-cat").orElseThrow();
        r1.setRecentCount(9999L); // change rank criteria by changing recent count

        // Fetch suggestions again (should hit CACHE and return original cached order, not Trie order)
        List<String> results2 = suggestionService.getSuggestions(prefix);
        assertEquals(results1, results2); // must match exactly since it's cached!
    }
}
