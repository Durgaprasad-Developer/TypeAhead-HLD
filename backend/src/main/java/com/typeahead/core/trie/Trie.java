package com.typeahead.core.trie;

import com.typeahead.core.dataset.QueryRecord;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Prefix-search Trie over query strings.
 *
 * <p>Responsibilities (DESIGN.md §9.3):
 * <ul>
 *   <li>{@link #insert} — add a query to the Trie, attaching a reference to its
 *       {@link QueryRecord} (the canonical count/timestamp object).</li>
 *   <li>{@link #findPrefix} — walk the Trie to the node at the end of a prefix,
 *       returning {@code null} if the prefix doesn't exist.</li>
 *   <li>{@link #collectSuggestions} — BFS/DFS from a given node to collect all
 *       {@link QueryRecord}s reachable from that subtree.</li>
 * </ul>
 *
 * <p>The Trie stores <strong>only references</strong> to {@link QueryRecord} objects
 * held in the {@link com.typeahead.core.dataset.QueryIndex} — never copies. This
 * means a count update in the QueryIndex is immediately visible via the Trie's
 * pointer; the two structures can never diverge.
 *
 * <p>Thread safety: the Trie is written once (at startup via
 * {@link com.typeahead.core.dataset.DatasetLoader}) and then read-only on the
 * hot path. New queries submitted via POST /search are inserted under a
 * {@code synchronized} block. This is sufficient because:
 * <ul>
 *   <li>Reads (prefix scan) are concurrent and non-mutating.</li>
 *   <li>Writes are rare (new unknown queries only; existing queries just update
 *       the mutable fields on the existing QueryRecord, not the Trie structure).</li>
 * </ul>
 *
 * <p>Zero Spring annotations — pure Java, unit-testable with {@code new Trie()}.
 */
public class Trie {

    private final TrieNode root = new TrieNode();

    /**
     * Insert a query string into the Trie and attach a reference to its record.
     *
     * @param query       The normalised (lowercase, trimmed) query string.
     * @param queryRecord The {@link QueryRecord} for this query. Must not be null.
     */
    public synchronized void insert(String query, QueryRecord queryRecord) {
        if (query == null || query.isEmpty()) return;

        TrieNode current = root;
        for (char c : query.toCharArray()) {
            current = current.addChild(c);
        }
        current.setWord(true);
        current.setQueryReference(queryRecord);
    }

    /**
     * Walk the Trie along {@code prefix} and return the node at the end of the prefix.
     *
     * @param prefix The prefix to search for.
     * @return The {@link TrieNode} at the end of the prefix, or {@code null} if no
     *         such prefix exists in the Trie.
     */
    public TrieNode findPrefix(String prefix) {
        if (prefix == null || prefix.isEmpty()) return root;

        TrieNode current = root;
        for (char c : prefix.toCharArray()) {
            current = current.getChild(c);
            if (current == null) return null;
        }
        return current;
    }

    /**
     * Collect all {@link QueryRecord}s reachable from {@code startNode} via any
     * descendant path, up to {@code limit} records.
     *
     * <p>Uses iterative DFS (stack-based) to avoid stack-overflow on deep Trie paths.
     * All QueryRecords with {@code isWord == true} encountered in the traversal are
     * added to the result list, unordered — the caller ({@code SuggestionRankingService})
     * is responsible for sorting by trending score.
     *
     * @param startNode The node to begin collection from (typically the result of
     *                  {@link #findPrefix}).
     * @param limit     Maximum number of records to return.  Use
     *                  {@link Integer#MAX_VALUE} for no limit.
     * @return A list of up to {@code limit} QueryRecords, in DFS traversal order
     *         (unsorted — ranked by the caller).
     */
    public List<QueryRecord> collectSuggestions(TrieNode startNode, int limit) {
        List<QueryRecord> results = new ArrayList<>();
        if (startNode == null) return results;

        Deque<TrieNode> stack = new ArrayDeque<>();
        stack.push(startNode);

        while (!stack.isEmpty() && results.size() < limit) {
            TrieNode node = stack.pop();

            if (node.isWord() && node.getQueryReference() != null) {
                results.add(node.getQueryReference());
            }

            // Push children onto the stack for further traversal.
            // Order doesn't matter here — ranking is done by the caller.
            for (TrieNode child : node.getChildren().values()) {
                stack.push(child);
            }
        }

        return results;
    }

    /** Exposed for testing: returns the root node so tests can inspect tree shape. */
    public TrieNode getRoot() {
        return root;
    }
}
