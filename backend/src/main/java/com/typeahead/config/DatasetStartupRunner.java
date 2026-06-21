package com.typeahead.config;

import com.typeahead.core.dataset.DatasetLoader;
import com.typeahead.core.dataset.QueryIndex;
import com.typeahead.core.trie.Trie;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Triggers dataset loading synchronously before the application accepts traffic.
 *
 * <p>Per DESIGN.md §9.5:
 * <pre>
 *   Spring Boot starts
 *     → DatasetLoader.load()
 *       → read processed CSV
 *       → for each row: create QueryRecord
 *       → insert into QueryIndex
 *       → insert into Trie
 *     → Application marked ready (health check passes only after this completes)
 * </pre>
 *
 * <p>{@link ApplicationRunner} is called after the Spring context is fully
 * initialized but before the embedded server starts accepting connections —
 * exactly the semantics we need.
 *
 * <p>If {@link DatasetLoader#load} throws (e.g. CSV missing), Spring propagates
 * the exception and the application fails to start entirely. This is the intended
 * "fail fast" behavior — never start with a half-loaded index.
 */
@Component
public class DatasetStartupRunner implements ApplicationRunner {

    private final DatasetLoader datasetLoader;
    private final Trie          trie;
    private final QueryIndex    queryIndex;

    public DatasetStartupRunner(DatasetLoader datasetLoader, Trie trie, QueryIndex queryIndex) {
        this.datasetLoader = datasetLoader;
        this.trie          = trie;
        this.queryIndex    = queryIndex;
    }

    @Override
    public void run(ApplicationArguments args) {
        datasetLoader.load(trie, queryIndex);
    }
}
