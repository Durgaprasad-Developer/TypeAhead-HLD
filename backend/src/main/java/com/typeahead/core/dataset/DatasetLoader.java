package com.typeahead.core.dataset;

import com.typeahead.core.trie.Trie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

/**
 * Loads the preprocessed query CSV into the in-memory Trie and QueryIndex
 * at application startup.
 *
 * <p>Per DESIGN.md §9.5, this runs synchronously before the app accepts traffic.
 * If the CSV is missing or corrupt, this method throws {@link DatasetLoadException}
 * and the application fails fast — it must not start in a half-loaded state.
 *
 * <p>The processed CSV format (comma-separated, written by {@code preprocess.py}):
 * <pre>
 *   query,count,lastSeen
 *   google,32396,2006-05-31
 *   yahoo,13344,2006-05-31
 *   ...
 * </pre>
 *
 * <p>Each row produces one {@link QueryRecord} that is:
 * <ul>
 *   <li>Inserted into the {@link QueryIndex} (O(1) exact-match lookup).</li>
 *   <li>Inserted into the {@link Trie} with a <em>reference</em> to the same
 *       record object — never a copy — so counts stay in sync automatically.</li>
 * </ul>
 *
 * <p>This class is a {@code @Component} so Spring can inject the config values,
 * but its {@link #load()} method is pure: it reads a file and populates
 * the two {@code core/} objects. No DB access happens here.
 */
@Component
public class DatasetLoader {

    private static final Logger log = LoggerFactory.getLogger(DatasetLoader.class);

    private static final int LOG_INTERVAL = 50_000;   // log every 50K rows

    @Value("${typeahead.dataset.processed-path}")
    private String processedPath;

    @Value("${typeahead.dataset.skip-on-startup:false}")
    private boolean skipOnStartup;

    /**
     * Read the processed CSV and populate the given Trie and QueryIndex.
     *
     * @param trie       The Trie to populate.
     * @param queryIndex The QueryIndex to populate.
     * @throws DatasetLoadException if the CSV is missing, unreadable, or fatally malformed.
     */
    public void load(Trie trie, QueryIndex queryIndex) {
        if (skipOnStartup) {
            log.info("DatasetLoader: skip-on-startup=true, skipping CSV load (test profile)");
            return;
        }

        // Resolve the path relative to the JVM working directory.
        // When launched via `mvn spring-boot:run` from backend/, the WD is backend/,
        // so a bare "dataset/processed/queries.csv" would not resolve.
        // The application.yml path should therefore be set to an absolute path, or
        // the app should be run from the repo root. We support both by trying the
        // configured path first; if it doesn't exist we look one level up (repo root).
        java.nio.file.Path csvPath = java.nio.file.Path.of(processedPath);
        if (!csvPath.isAbsolute() && !java.nio.file.Files.exists(csvPath)) {
            // Try resolving from parent directory (repo root when running from backend/)
            java.nio.file.Path parentTry = java.nio.file.Path.of("..").resolve(processedPath).normalize();
            if (java.nio.file.Files.exists(parentTry)) {
                csvPath = parentTry;
                log.info("DatasetLoader: resolved CSV path to {}", csvPath.toAbsolutePath());
            }
        }

        String resolvedPath = csvPath.toAbsolutePath().toString();
        log.info("DatasetLoader: loading from {}", resolvedPath);
        long startMs = System.currentTimeMillis();

        int loaded  = 0;
        int skipped = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(resolvedPath))) {
            String header = reader.readLine();  // skip header row "query,count,lastSeen"
            if (header == null) {
                throw new DatasetLoadException("Processed CSV is empty: " + processedPath);
            }

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;

                QueryRecord record = parseLine(line);
                if (record == null) {
                    skipped++;
                    continue;
                }

                queryIndex.insert(record.getQuery(), record);
                trie.insert(record.getQuery(), record);   // stores reference, not copy
                loaded++;

                if (loaded % LOG_INTERVAL == 0) {
                    log.info("DatasetLoader: loaded {} queries...", loaded);
                }
            }
        } catch (IOException e) {
            throw new DatasetLoadException("Failed to read processed CSV: " + processedPath, e);
        }

        long durationMs = System.currentTimeMillis() - startMs;
        log.info("DatasetLoader: complete — {} queries loaded, {} skipped, took {}ms",
                 loaded, skipped, durationMs);

        if (loaded == 0) {
            throw new DatasetLoadException(
                "DatasetLoader loaded 0 queries from " + processedPath +
                ". The file may be empty or all rows were malformed. " +
                "Run dataset/preprocess.py first.");
        }
    }

    /**
     * Parse a single CSV line into a {@link QueryRecord}.
     *
     * <p>Format: {@code query,count,lastSeen}
     * where {@code lastSeen} is optional and may be empty.
     *
     * @param line A raw line from the processed CSV.
     * @return A populated {@link QueryRecord}, or {@code null} if the line is malformed.
     */
    private QueryRecord parseLine(String line) {
        // Split into at most 3 parts — query may contain commas if ever quoted,
        // but our preprocess.py outputs simple unquoted strings so this is safe.
        // We split on ',' with a limit so a query containing commas in future
        // doesn't silently truncate.
        String[] parts = line.split(",", 3);
        if (parts.length < 2) {
            log.debug("DatasetLoader: skipping malformed line (< 2 fields): {}", line);
            return null;
        }

        String query = parts[0].trim();
        if (query.isEmpty()) {
            return null;
        }

        long count;
        try {
            count = Long.parseLong(parts[1].trim());
        } catch (NumberFormatException e) {
            log.debug("DatasetLoader: skipping line with non-numeric count: {}", line);
            return null;
        }

        Instant lastSearched = Instant.now();
        if (parts.length >= 3 && !parts[2].trim().isEmpty()) {
            try {
                lastSearched = LocalDate.parse(parts[2].trim())
                                        .atStartOfDay(ZoneOffset.UTC)
                                        .toInstant();
            } catch (DateTimeParseException e) {
                // lastSearched defaults to now — not fatal
            }
        }

        // recentCount starts at 0 for historical data; TrendingService will
        // populate it on the first recompute cycle using search event timestamps.
        return new QueryRecord(query, count, 0L, lastSearched);
    }
}
