package com.typeahead.core.batch;

import com.typeahead.entity.SearchQuery;
import com.typeahead.repository.SearchQueryRepository;
import com.typeahead.service.SearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class BatchWriteIntegrationTest {

    @Autowired
    private SearchService searchService;

    @Autowired
    private BatchQueue batchQueue;

    @Autowired
    private BatchWriter batchWriter;

    @Autowired
    private SearchQueryRepository repository;

    @BeforeEach
    void setUp() {
        // Clear queue by draining
        java.util.List<String> drained = new java.util.ArrayList<>();
        batchQueue.drainTo(drained, 10000);
    }

    @Test
    void testBatchAggregationAndPersistence() {
        // 1. Submit search queries
        // "batchtest-a" x3
        searchService.search("batchtest-a");
        searchService.search("batchtest-a");
        searchService.search("batchtest-a");

        // "batchtest-b" x2
        searchService.search("batchtest-b");
        searchService.search("batchtest-b");

        // Check queue size
        assertEquals(5, batchQueue.size());

        // 2. Trigger BatchWriter flush manually
        batchWriter.flushBatch();

        // Check queue is now empty
        assertEquals(0, batchQueue.size());

        // 3. Verify counts in the database
        Optional<SearchQuery> queryA = repository.findByQuery("batchtest-a");
        assertTrue(queryA.isPresent());
        assertEquals(3L, queryA.get().getOverallCount());
        assertEquals(3L, queryA.get().getRecentCount());

        Optional<SearchQuery> queryB = repository.findByQuery("batchtest-b");
        assertTrue(queryB.isPresent());
        assertEquals(2L, queryB.get().getOverallCount());
        assertEquals(2L, queryB.get().getRecentCount());

        // 4. Submit more queries and flush again to test updating
        searchService.search("batchtest-a");
        batchWriter.flushBatch();

        queryA = repository.findByQuery("batchtest-a");
        assertTrue(queryA.isPresent());
        assertEquals(4L, queryA.get().getOverallCount()); // 3 + 1 = 4
    }
}
