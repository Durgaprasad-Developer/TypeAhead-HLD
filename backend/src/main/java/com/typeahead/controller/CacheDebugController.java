package com.typeahead.controller;

import com.typeahead.dto.CacheDebugResponse;
import com.typeahead.core.cache.CacheNode;
import com.typeahead.core.cache.ConsistentHashRing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller exposing consistent hash ring and cache nodes introspection endpoint.
 *
 * <p>Per DESIGN.md §11.3, provides `GET /cache/debug` to allow visualization of keys
 * across shards on the frontend dashboard.
 */
@RestController
@RequestMapping("/cache/debug")
public class CacheDebugController {

    private static final Logger log = LoggerFactory.getLogger(CacheDebugController.class);

    private final ConsistentHashRing consistentHashRing;

    public CacheDebugController(ConsistentHashRing consistentHashRing) {
        this.consistentHashRing = consistentHashRing;
    }

    /**
     * Dumps the current consistent hash ring size, registered nodes, and their cached values,
     * or inspects consistent hash routing for a single prefix query.
     *
     * @param prefix Optional prefix to simulate routing.
     * @return CacheDebugResponse DTO or prefix routing detail Map.
     */
    @GetMapping
    public ResponseEntity<?> getCacheDebugInfo(@RequestParam(value = "prefix", required = false) String prefix) {
        if (prefix != null && !prefix.trim().isEmpty()) {
            String normalized = prefix.trim().toLowerCase();
            log.info("CacheDebugController: simulating routing for prefix '{}'", normalized);

            CacheNode node = consistentHashRing.getNode(normalized);
            long hash = ((long) com.typeahead.core.hash.MurmurHash3.hash32(normalized)) & 0xFFFFFFFFL;
            boolean isCached = false;
            List<String> suggestions = java.util.Collections.emptyList();

            if (node != null) {
                List<String> cached = node.get(normalized);
                if (cached != null) {
                    isCached = true;
                    suggestions = cached;
                }
            }

            Map<String, Object> routingDetail = new HashMap<>();
            routingDetail.put("prefix", normalized);
            routingDetail.put("assignedNode", node != null ? node.getId() : null);
            routingDetail.put("assignedNodeName", node != null ? node.getName() : null);
            routingDetail.put("hash", hash);
            routingDetail.put("isCached", isCached);
            routingDetail.put("suggestions", suggestions);

            return ResponseEntity.ok(routingDetail);
        }

        log.info("CacheDebugController: fetching cache debug dump");

        List<CacheDebugResponse.NodeDebugInfo> nodeInfos = consistentHashRing.getPhysicalNodes().stream()
            .map(node -> new CacheDebugResponse.NodeDebugInfo(
                node.getId(),
                node.getName(),
                node.size(),
                node.getSnapshot()
            ))
            .collect(Collectors.toList());

        CacheDebugResponse response = new CacheDebugResponse(nodeInfos, consistentHashRing.size());
        return ResponseEntity.ok(response);
    }
}
