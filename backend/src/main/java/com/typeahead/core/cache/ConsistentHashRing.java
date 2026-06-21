package com.typeahead.core.cache;

import com.typeahead.core.hash.MurmurHash3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Consistent Hash Ring implementation using 32-bit MurmurHash3.
 *
 * <p>Per DESIGN.md §10.2, virtual nodes are used to balance the distribution of keys
 * across physical cache nodes.
 *
 * <p>Thread safety: Synchronized on internal ring operations.
 */
public class ConsistentHashRing {

    private static final Logger log = LoggerFactory.getLogger(ConsistentHashRing.class);

    private final TreeMap<Long, CacheNode> ring;
    private final int                      virtualNodesCount;
    private final Set<CacheNode>           physicalNodes;

    public ConsistentHashRing(int virtualNodesCount) {
        this.ring              = new TreeMap<>();
        this.virtualNodesCount = virtualNodesCount;
        this.physicalNodes     = new HashSet<>();
    }

    /**
     * Add a physical cache node to the ring, generating virtual nodes for it.
     *
     * @param node The physical cache node.
     */
    public synchronized void addNode(CacheNode node) {
        if (node == null) return;
        physicalNodes.add(node);

        for (int i = 0; i < virtualNodesCount; i++) {
            // Generate virtual node string name
            String vNodeKey = node.getId() + "-vnode-" + i;
            long hash = getUnsignedHash(vNodeKey);
            ring.put(hash, node);
        }
        log.info("ConsistentHashRing: added physical node '{}' (id={}) with {} virtual nodes",
                 node.getName(), node.getId(), virtualNodesCount);
    }

    /**
     * Remove a physical cache node and all its virtual nodes from the ring.
     *
     * @param node The physical cache node to remove.
     */
    public synchronized void removeNode(CacheNode node) {
        if (node == null) return;
        physicalNodes.remove(node);

        for (int i = 0; i < virtualNodesCount; i++) {
            String vNodeKey = node.getId() + "-vnode-" + i;
            long hash = getUnsignedHash(vNodeKey);
            ring.remove(hash);
        }
        log.info("ConsistentHashRing: removed physical node '{}'", node.getId());
    }

    /**
     * Route a request key (prefix) to the appropriate cache node on the ring.
     *
     * @param key The cache routing key (e.g. search prefix query).
     * @return The routed CacheNode, or null if the ring is empty.
     */
    public synchronized CacheNode getNode(String key) {
        if (ring.isEmpty() || key == null) {
            return null;
        }

        long hash = getUnsignedHash(key);
        if (!ring.containsKey(hash)) {
            // Find the closest hash key value greater than or equal to current hash
            SortedMap<Long, CacheNode> tailMap = ring.tailMap(hash);
            hash = tailMap.isEmpty() ? ring.firstKey() : tailMap.firstKey();
        }
        return ring.get(hash);
    }

    /** Helper to compute 32-bit unsigned hash. */
    private long getUnsignedHash(String key) {
        return ((long) MurmurHash3.hash32(key)) & 0xFFFFFFFFL;
    }

    /** Returns all unique physical nodes currently registered on the ring. */
    public synchronized Collection<CacheNode> getPhysicalNodes() {
        return Collections.unmodifiableCollection(new ArrayList<>(physicalNodes));
    }

    /** Returns current ring size (total virtual nodes). */
    public synchronized int size() {
        return ring.size();
    }
}
