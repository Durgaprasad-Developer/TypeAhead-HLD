package com.typeahead.controller;

import com.typeahead.core.dataset.QueryIndex;
import com.typeahead.core.dataset.QueryRecord;
import com.typeahead.core.trie.Trie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SearchControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Trie trie;

    @Autowired
    private QueryIndex queryIndex;

    @BeforeEach
    void setUp() {
        // Insert starting values for reranking test
        insertQuery("rerankprefix-a", 10, 1); // score: 0.3*10 + 0.7*1 = 3.7
        insertQuery("rerankprefix-b", 20, 2); // score: 0.3*20 + 0.7*2 = 7.4
    }

    private void insertQuery(String q, long overall, long recent) {
        QueryRecord record = new QueryRecord(q, overall, recent, Instant.now());
        queryIndex.insert(q, record);
        trie.insert(q, record);
    }

    @Test
    void postSearch_newQuery_insertsAndReturnsSearched() throws Exception {
        // Submit search for a new query
        mockMvc.perform(post("/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\": \"newqueryprefix-item\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Searched")));

        // Verify it was inserted in-memory
        assertTrue(queryIndex.exists("newqueryprefix-item"));
        QueryRecord record = queryIndex.find("newqueryprefix-item").orElseThrow();
        assertEquals(1, record.getOverallCount());
        assertEquals(1, record.getRecentCount());

        // Verify it is returned in suggestion list
        mockMvc.perform(get("/suggest")
                .param("q", "newqueryprefix-item")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", contains("newqueryprefix-item")));
    }

    @Test
    void postSearch_existingQuery_incrementsCountsAndReranks() throws Exception {
        // Initially, b (score 7.4) outranks a (score 3.7)
        mockMvc.perform(get("/suggest")
                .param("q", "rerankprefix")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", contains("rerankprefix-b", "rerankprefix-a")));

        // Search for 'a' multiple times to increment its score
        for (int i = 0; i < 20; i++) {
            mockMvc.perform(post("/search")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"query\": \"rerankprefix-a\"}"))
                    .andExpect(status().isOk());
        }

        // 'a' count should have increased, making its score higher than 'b'
        mockMvc.perform(get("/suggest")
                .param("q", "rerankprefix")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", contains("rerankprefix-a", "rerankprefix-b")));
    }

    @Test
    void postSearch_invalidQuery_returns400() throws Exception {
        mockMvc.perform(post("/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Bad Request")));
    }
}
