package com.typeahead.service;

import com.typeahead.core.cache.CacheNode;
import com.typeahead.core.cache.ConsistentHashRing;
import com.typeahead.core.dataset.QueryRecord;
import com.typeahead.core.ranking.SuggestionRankingService;
import com.typeahead.core.trie.Trie;
import com.typeahead.core.trie.TrieNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service to orchestrate the suggestion prefix search and ranking.
 *
 * <p>Integrates a distributed consistent hash ring of cache nodes to intercept lookups
 * before scanning the in-memory Trie.
 */
@Service
public class SuggestionService {

    private static final Logger log = LoggerFactory.getLogger(SuggestionService.class);

    private final Trie                     trie;
    private final SuggestionRankingService rankingService;
    private final ConsistentHashRing       consistentHashRing;

    public SuggestionService(Trie trie,
                             SuggestionRankingService rankingService,
                             ConsistentHashRing consistentHashRing) {
        this.trie               = trie;
        this.rankingService     = rankingService;
        this.consistentHashRing = consistentHashRing;
    }

    /**
     * Look up and rank suggestions starting with the given prefix.
     *
     * <p>Checks the consistent hashing ring cache first. If a cache hit occurs, returns
     * immediately. Otherwise, scans the Trie, computes the top 10 ranked results, caches them
     * in the routed node, and returns.
     *
     * @param prefix The query prefix typed by the user.
     * @return A list of at most 10 ranked query suggestion strings.
     */
    public List<String> getSuggestions(String prefix) {
        if (prefix == null || prefix.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String normalized = prefix.trim().toLowerCase();
        log.debug("SuggestionService: fetching suggestions for prefix '{}'", normalized);

        // 1. Route to CacheNode using consistent hash ring
        CacheNode routedNode = consistentHashRing.getNode(normalized);
        if (routedNode != null) {
            List<String> cached = routedNode.get(normalized);
            if (cached != null) {
                log.info("SuggestionService: Cache HIT on '{}' for prefix '{}'", routedNode.getId(), normalized);
                return cached;
            }
            log.info("SuggestionService: Cache MISS on '{}' for prefix '{}'", routedNode.getId(), normalized);
        }

        // 2. Trie prefix scan on cache miss
        TrieNode startNode = trie.findPrefix(normalized);
        if (startNode == null) {
            List<String> emptyResult = Collections.emptyList();
            if (routedNode != null) {
                routedNode.put(normalized, emptyResult);
            }
            return emptyResult;
        }

        // Collect all QueryRecords matching the prefix
        List<QueryRecord> records = trie.collectSuggestions(startNode, Integer.MAX_VALUE);

        // Sort by trending score and return top 10
        List<QueryRecord> top10 = rankingService.topN(records, 10);
        List<String> suggestions = top10.stream()
                                         .map(QueryRecord::getQuery)
                                         .collect(Collectors.toList());

        // 3. Cache the suggestions back in the routed node
        if (routedNode != null) {
            routedNode.put(normalized, suggestions);
        }

        return suggestions;
    }
}
