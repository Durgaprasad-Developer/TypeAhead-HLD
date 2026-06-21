package com.typeahead;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the Search Typeahead System.
 *
 * <p>On startup:
 * <ol>
 *   <li>Spring context is loaded (config, services, repositories).</li>
 *   <li>DatasetLoader runs synchronously via ApplicationRunner — the app does NOT
 *       accept traffic until the Trie + QueryIndex are fully populated.</li>
 *   <li>BatchWriter and TrendingService schedulers begin running.</li>
 * </ol>
 */
@SpringBootApplication
@EnableScheduling
public class TypeaheadApplication {

    public static void main(String[] args) {
        SpringApplication.run(TypeaheadApplication.class, args);
    }
}
