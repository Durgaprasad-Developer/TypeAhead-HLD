package com.typeahead.core.batch;

import com.typeahead.repository.SearchQueryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Background worker task that drains the {@link BatchQueue} and writes updates in batches.
 *
 * <p>Per DESIGN.md §9.4, runs periodically on a schedule. It groups multiple searches for
 * the same query in the batch to reduce total database roundtrips and locks, then uses
 * native database upserts to write to PostgreSQL.
 */
@Component
public class BatchWriter {

    private static final Logger log = LoggerFactory.getLogger(BatchWriter.class);

    private final BatchQueue            batchQueue;
    private final SearchQueryRepository searchQueryRepository;
    private final int                   maxSize;

    public BatchWriter(BatchQueue batchQueue,
                       SearchQueryRepository searchQueryRepository,
                       @Value("${typeahead.batch.max-size:500}") int maxSize) {
        this.batchQueue            = batchQueue;
        this.searchQueryRepository = searchQueryRepository;
        this.maxSize               = maxSize;
    }

    /**
     * Periodically flushes the enqueued search counts to PostgreSQL.
     * Uses the fixedDelayString from application properties (converted to milliseconds).
     */
    @Scheduled(fixedDelayString = "#{T(java.lang.Integer).parseInt('${typeahead.batch.interval-seconds:10}') * 1000}")
    @Transactional
    public void flushBatch() {
        int queueSize = batchQueue.size();
        if (queueSize == 0) {
            return;
        }

        log.debug("BatchWriter: flushing batch, queue size={}", queueSize);
        List<String> drained = new ArrayList<>();
        batchQueue.drainTo(drained, maxSize);

        if (drained.isEmpty()) {
            return;
        }

        // Group searches in the batch to update counts in a single statement per query
        Map<String, Long> queryCounts = drained.stream()
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        log.info("BatchWriter: writing {} raw updates aggregated into {} distinct queries",
                 drained.size(), queryCounts.size());

        Instant now = Instant.now();
        int updatedCount = 0;

        for (Map.Entry<String, Long> entry : queryCounts.entrySet()) {
            try {
                searchQueryRepository.upsertQueryCount(entry.getKey(), entry.getValue(), now);
                updatedCount++;
            } catch (Exception e) {
                log.error("BatchWriter: failed to upsert count for query '{}'", entry.getKey(), e);
            }
        }

        log.debug("BatchWriter: completed batch update of {} queries", updatedCount);
    }
}
