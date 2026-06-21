package com.typeahead.config;

import com.typeahead.core.cache.CacheNode;
import com.typeahead.core.cache.ConsistentHashRing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Spring configuration class for initializing the consistent hash ring and cache nodes.
 *
 * <p>Per DESIGN.md §10.3, configures physical {@link CacheNode} shards and adds
 * them to the {@link ConsistentHashRing} singleton.
 */
@Configuration
public class CacheConfig {

    private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

    @Value("${typeahead.cache.node-count:3}")
    private int nodeCount;

    @Value("${typeahead.cache.virtual-nodes-per-node:150}")
    private int virtualNodesPerNode;

    @Bean
    public List<CacheNode> cacheNodes() {
        log.info("CacheConfig: creating {} in-memory physical CacheNode instances", nodeCount);
        List<CacheNode> nodes = new ArrayList<>();
        for (int i = 0; i < nodeCount; i++) {
            String nodeId = "node-" + i;
            String nodeName = "Cache-Node-" + i;
            nodes.add(new CacheNode(nodeId, nodeName));
        }
        return nodes;
    }

    @Bean
    public ConsistentHashRing consistentHashRing(List<CacheNode> cacheNodes) {
        log.info("CacheConfig: initialising ConsistentHashRing with {} virtual nodes per physical node",
                 virtualNodesPerNode);
        ConsistentHashRing ring = new ConsistentHashRing(virtualNodesPerNode);
        for (CacheNode node : cacheNodes) {
            ring.addNode(node);
        }
        return ring;
    }
}
