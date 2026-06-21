package com.typeahead.core.cache;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory representation of a single physical cache node (shard).
 *
 * <p>Per DESIGN.md §10.1, in this architecture, cache nodes run in-process as distinct
 * objects containing separate concurrent maps to simulate a sharded cluster.
 */
public class CacheNode {

    private final String                                id;
    private final String                                name;
    private final ConcurrentHashMap<String, List<String>> store;

    public CacheNode(String id, String name) {
        this.id    = id;
        this.name  = name;
        this.store = new ConcurrentHashMap<>();
    }

    public String getId()   { return id; }
    public String getName() { return name; }

    /** Look up suggestions for a prefix in this cache node. */
    public List<String> get(String prefix) {
        return store.get(prefix);
    }

    /** Cache the suggestions for a prefix in this node. */
    public void put(String prefix, List<String> suggestions) {
        if (prefix != null && suggestions != null) {
            store.put(prefix, suggestions);
        }
    }

    /** Evict a prefix cache entry from this node. */
    public void invalidate(String prefix) {
        if (prefix != null) {
            store.remove(prefix);
        }
    }

    /** Clear all cached contents in this node. */
    public void clear() {
        store.clear();
    }

    /** Returns total number of cached entries in this node. */
    public int size() {
        return store.size();
    }

    /** Returns a read-only snapshot of the backing map (for debugging/introspection). */
    public ConcurrentHashMap<String, List<String>> getSnapshot() {
        return store;
    }

    @Override
    public String toString() {
        return "CacheNode{id='" + id + "', name='" + name + "', size=" + store.size() + "}";
    }
}
