package com.typeahead.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CacheDebugControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getCacheDebugInfo_returnsValidSchema() throws Exception {
        mockMvc.perform(get("/cache/debug")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.hashRingSize").value(450))
                .andExpect(jsonPath("$.nodes").isArray())
                .andExpect(jsonPath("$.nodes[0].nodeId").exists())
                .andExpect(jsonPath("$.nodes[0].nodeName").exists())
                .andExpect(jsonPath("$.nodes[0].cacheSize").exists())
                .andExpect(jsonPath("$.nodes[0].entries").exists());
    }
}
