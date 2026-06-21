package com.typeahead.core.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Thread-safe buffer queue for incoming search query updates.
 *
 * <p>Per DESIGN.md §9.3, instead of writing every POST /search request directly
 * to the database (which would cause database connection pool exhaustion and high write locks),
 * search queries are enqueued here and written to PostgreSQL in batches by {@link BatchWriter}.
 */
@Component
public class BatchQueue {

    private static final Logger log = LoggerFactory.getLogger(BatchQueue.class);

    private final LinkedBlockingQueue<String> queue;

    public BatchQueue(@Value("${typeahead.batch.max-size:500}") int capacity) {
        // Allow the queue to grow larger than the single batch write size
        // to handle brief bursts, e.g., capacity = max-size * 10
        int queueCapacity = capacity * 10;
        log.info("BatchQueue: initialising queue with capacity {}", queueCapacity);
        this.queue = new LinkedBlockingQueue<>(queueCapacity);
    }

    /**
     * Enqueue a search query for asynchronous write.
     *
     * @param query The normalized query.
     * @return {@code true} if enqueued, {@code false} if the queue is full.
     */
    public boolean enqueue(String query) {
        boolean success = queue.offer(query);
        if (!success) {
            log.warn("BatchQueue: Queue is full! Query '{}' dropped to protect system memory.", query);
        }
        return success;
    }

    /**
     * Drain up to {@code limit} elements from the queue into the given list.
     *
     * @param buffer The destination list.
     * @param limit  Maximum number of elements to drain.
     * @return Number of elements drained.
     */
    public int drainTo(List<String> buffer, int limit) {
        return queue.drainTo(buffer, limit);
    }

    /** Returns current queue size (for monitoring/debugging). */
    public int size() {
        return queue.size();
    }
}
