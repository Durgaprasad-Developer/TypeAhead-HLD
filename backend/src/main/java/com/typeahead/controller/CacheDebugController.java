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
     * Dumps the current consistent hash ring size, registered nodes, and their cached values.
     *
     * @return CacheDebugResponse DTO.
     */
    @GetMapping
    public CacheDebugResponse getCacheDebugInfo() {
        log.info("CacheDebugController: fetching cache debug dump");

        List<CacheDebugResponse.NodeDebugInfo> nodeInfos = consistentHashRing.getPhysicalNodes().stream()
            .map(node -> new CacheDebugResponse.NodeDebugInfo(
                node.getId(),
                node.getName(),
                node.size(),
                node.getSnapshot()
            ))
            .collect(Collectors.toList());

        return new CacheDebugResponse(nodeInfos, consistentHashRing.size());
    }
}
