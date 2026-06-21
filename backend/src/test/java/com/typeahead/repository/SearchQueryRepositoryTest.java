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
    void findByQuery_nonExistent_returnsEmpty() {
        Optional<SearchQuery> fetched = repository.findByQuery("nonexistent");
        assertFalse(fetched.isPresent());
    }
}
