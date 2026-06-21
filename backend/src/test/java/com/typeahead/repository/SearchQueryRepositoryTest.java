package com.typeahead.repository;

import com.typeahead.entity.SearchQuery;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class SearchQueryRepositoryTest {

    @Autowired
    private SearchQueryRepository repository;

    @Autowired
    private org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager entityManager;

    @Test
    void saveAndFindByQuery() {
        SearchQuery query = new SearchQuery("testquery", 100L, 10L, Instant.now());
        SearchQuery saved = repository.save(query);
        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());

        Optional<SearchQuery> fetchedOpt = repository.findByQuery("testquery");
        assertTrue(fetchedOpt.isPresent());
        SearchQuery fetched = fetchedOpt.get();
        assertEquals("testquery", fetched.getQuery());
        assertEquals(100L, fetched.getOverallCount());
        assertEquals(10L, fetched.getRecentCount());
    }

    @Test
    void upsertQueryCount_insertsOrUpdates() {
        Instant now = Instant.now();
        
        // 1. Insert new query via upsert
        int inserted = repository.upsertQueryCount("upsertedquery", 5L, now);
        assertEquals(1, inserted);

        // Clear persistence context so finding it fetches it fresh from DB
        entityManager.clear();

        Optional<SearchQuery> fetchedOpt = repository.findByQuery("upsertedquery");
        assertTrue(fetchedOpt.isPresent());
        SearchQuery fetched = fetchedOpt.get();
        assertEquals(5L, fetched.getOverallCount());
        assertEquals(5L, fetched.getRecentCount());

        // 2. Increment query via upsert
        int updated = repository.upsertQueryCount("upsertedquery", 3L, now);
        assertTrue(updated >= 1);

        // Clear persistence context again
        entityManager.clear();

        fetchedOpt = repository.findByQuery("upsertedquery");
        assertTrue(fetchedOpt.isPresent());
        fetched = fetchedOpt.get();
        assertEquals(8L, fetched.getOverallCount()); // 5 + 3 = 8
        assertEquals(8L, fetched.getRecentCount());  // 5 + 3 = 8
    }

    @Test
    void findByQuery_nonExistent_returnsEmpty() {
        Optional<SearchQuery> fetched = repository.findByQuery("nonexistent");
        assertFalse(fetched.isPresent());
    }
}
