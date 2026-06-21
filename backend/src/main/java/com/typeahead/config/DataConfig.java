package com.typeahead.config;

import com.typeahead.core.dataset.DatasetLoader;
import com.typeahead.core.dataset.QueryIndex;
import com.typeahead.core.trie.Trie;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for the core in-memory data structures.
 *
 * <p>The {@link Trie} and {@link QueryIndex} are singletons — one shared instance
 * per JVM, populated once at startup by {@link DatasetLoader} and then used
 * read-mostly on every request. Declaring them as {@code @Bean}s here lets Spring
 * inject them into services without those services needing to know how they're
 * constructed.
 *
 * <p>Dependency direction enforced (DESIGN.md §13.3):
 * {@code Config → core/*} only. No {@code @Repository} or {@code @Service}
 * references here.
 */
@Configuration
public class DataConfig {

    @Bean
    public Trie trie() {
        return new Trie();
    }

    @Bean
    public QueryIndex queryIndex() {
        return new QueryIndex();
    }
}
