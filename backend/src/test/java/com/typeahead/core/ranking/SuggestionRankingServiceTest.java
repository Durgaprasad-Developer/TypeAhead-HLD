package com.typeahead.core.ranking;

import com.typeahead.core.dataset.QueryRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SuggestionRankingServiceTest {

    private SuggestionRankingService rankingService;

    @BeforeEach
    void setUp() {
        // Use 0.3 overall and 0.7 recent as defined in design
        rankingService = new SuggestionRankingService(0.3, 0.7);
    }

    private QueryRecord rec(String query, long overall, long recent) {
        return new QueryRecord(query, overall, recent, Instant.now());
    }

    @Test
    void calculateTrendingScore_correctCalculation() {
        QueryRecord r = rec("iphone", 100, 10);
        // Score = 0.3 * 100 + 0.7 * 10 = 30 + 7 = 37.0
        double score = rankingService.calculateTrendingScore(r);
        assertEquals(37.0, score, 0.0001);
    }

    @Test
    void sort_descendingTrendingScore() {
        List<QueryRecord> list = new ArrayList<>();
        list.add(rec("low", 10, 1));     // Score = 3 + 0.7 = 3.7
        list.add(rec("high", 100, 50));  // Score = 30 + 35 = 65.0
        list.add(rec("mid", 50, 10));    // Score = 15 + 7 = 22.0

        rankingService.sort(list);

        assertEquals("high", list.get(0).getQuery());
        assertEquals("mid", list.get(1).getQuery());
        assertEquals("low", list.get(2).getQuery());
    }

    @Test
    void sort_alphabeticalTieBreaker() {
        List<QueryRecord> list = new ArrayList<>();
        list.add(rec("beta", 100, 10));   // Score = 37.0
        list.add(rec("alpha", 100, 10));  // Score = 37.0
        list.add(rec("gamma", 100, 10));  // Score = 37.0

        rankingService.sort(list);

        // alphabetical: alpha -> beta -> gamma
        assertEquals("alpha", list.get(0).getQuery());
        assertEquals("beta", list.get(1).getQuery());
        assertEquals("gamma", list.get(2).getQuery());
    }

    @Test
    void topN_limitsResultsAndSorts() {
        List<QueryRecord> list = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            list.add(rec("q" + i, i * 10, i));
        }

        // q20 has highest counts, so it will have highest score
        List<QueryRecord> result = rankingService.topN(list, 10);

        assertEquals(10, result.size());
        assertEquals("q20", result.get(0).getQuery());
        assertEquals("q11", result.get(9).getQuery());
    }

    @Test
    void topN_handlesUnderflow() {
        List<QueryRecord> list = new ArrayList<>();
        list.add(rec("a", 1, 1));
        list.add(rec("b", 2, 2));

        List<QueryRecord> result = rankingService.topN(list, 10);

        assertEquals(2, result.size());
        assertEquals("b", result.get(0).getQuery());
    }
}
