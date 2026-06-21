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
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SuggestionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Trie trie;

    @Autowired
    private QueryIndex queryIndex;

    @BeforeEach
    void setUp() {
        // Clear or populate the Trie/QueryIndex for testing.
        // Since Trie does not have a clear method, we can just insert fresh elements,
        // or check if they are already present. Let's insert fresh ones for our prefix.
        // We'll use a prefix unique to this test, e.g. "integrationprefix".
        insertQuery("integrationprefix-a", 100, 10);
        insertQuery("integrationprefix-c", 300, 30);
        insertQuery("integrationprefix-b", 200, 20);
    }

    private void insertQuery(String q, long overall, long recent) {
        QueryRecord record = new QueryRecord(q, overall, recent, Instant.now());
        queryIndex.insert(q, record);
        trie.insert(q, record);
    }

    @Test
    void getSuggest_returnsRankedSuggestions() throws Exception {
        // Expected order by trending score (0.3 * overall + 0.7 * recent):
        // c: 0.3*300 + 0.7*30 = 90 + 21 = 111.0
        // b: 0.3*200 + 0.7*20 = 60 + 14 = 74.0
        // a: 0.3*100 + 0.7*10 = 30 + 7 = 37.0
        // Result order: integrationprefix-c, integrationprefix-b, integrationprefix-a
        mockMvc.perform(get("/suggest")
                .param("q", "integrationprefix")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", contains(
                        "integrationprefix-c",
                        "integrationprefix-b",
                        "integrationprefix-a"
                )));
    }

    @Test
    void getSuggest_emptyQuery_returnsEmptyArray() throws Exception {
        mockMvc.perform(get("/suggest")
                .param("q", "")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", empty()));
    }

    @Test
    void getSuggest_noMatches_returnsEmptyArray() throws Exception {
        mockMvc.perform(get("/suggest")
                .param("q", "nonexistentprefixxyz")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", empty()));
    }
}
