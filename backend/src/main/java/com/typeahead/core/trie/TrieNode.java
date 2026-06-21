package com.typeahead.core.trie;

import java.util.HashMap;
import java.util.Map;

/**
 * A single node in the Trie.
 *
 * <p>Children are stored in a {@link HashMap} keyed by {@code Character} — NOT a
 * fixed 26-slot array. This is a hard requirement (DESIGN.md §9.3): real AOL queries
 * contain spaces, digits, hyphens, dots, etc., so a letter-only array would silently
 * drop entire subtrees of the dataset.
 *
 * <p>Zero Spring annotations — this class is plain Java and must be constructable
 * with {@code new TrieNode()} in unit tests without a Spring context (DESIGN.md §13.1).
 */
public class TrieNode {

    /** Children indexed by the next character in the query string. */
    private final Map<Character, TrieNode> children = new HashMap<>();

    /**
     * True if this node marks the end of a complete query string.
     * (A node may be both a word end and an interior node for longer queries.)
     */
    private boolean isWord;

    /**
     * Reference to the {@link com.typeahead.core.dataset.QueryRecord} for this
     * query — only non-null when {@code isWord == true}.
     *
     * <p>This is a pointer, NOT a copy. Count/timestamp data lives exclusively in
     * the QueryIndex's QueryRecord; the Trie node holds a reference so both
     * structures always agree on the current value without a sync step.
     */
    private com.typeahead.core.dataset.QueryRecord queryReference;

    // ── Accessors ──────────────────────────────────────────────────────────

    public Map<Character, TrieNode> getChildren() {
        return children;
    }

    public TrieNode getChild(char c) {
        return children.get(c);
    }

    public TrieNode addChild(char c) {
        return children.computeIfAbsent(c, k -> new TrieNode());
    }

    public boolean isWord() {
        return isWord;
    }

    public void setWord(boolean word) {
        isWord = word;
    }

    public com.typeahead.core.dataset.QueryRecord getQueryReference() {
        return queryReference;
    }

    public void setQueryReference(com.typeahead.core.dataset.QueryRecord queryReference) {
        this.queryReference = queryReference;
    }

    public boolean hasChild(char c) {
        return children.containsKey(c);
    }
}
