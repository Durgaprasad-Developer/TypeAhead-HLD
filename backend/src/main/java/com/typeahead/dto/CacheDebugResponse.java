package com.typeahead.dto;

import java.util.List;
import java.util.Map;

/**
 * DTO representing the debug/introspection dump of the consistent hash ring and cache shards.
 *
 * <p>Per DESIGN.md §11.3:
 * <pre>
 * {
 *   "nodes": [
 *     {
 *       "nodeId": "node-0",
 *       "nodeName": "Cache-Node-0",
 *       "cacheSize": 1,
 *       "entries": {
 *         "ap": ["apple", "apricot"]
 *       }
 *     }
 *   ],
 *   "hashRingSize": 450
 * }
 * </pre>
 */
public record CacheDebugResponse(
    List<NodeDebugInfo> nodes,
    int hashRingSize
) {
    public record NodeDebugInfo(
        String nodeId,
        String nodeName,
        int cacheSize,
        Map<String, List<String>> entries
    ) {}
}
