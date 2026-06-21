package com.typeahead.core.ranking;

import com.typeahead.core.dataset.QueryRecord;

import java.util.ArrayList;
import java.util.List;

/**
 * Ranks and sorts query suggestions based on historical and recent activity.
 *
 * <p>Per DESIGN.md §12.1, the trending score formula is:
 * <pre>
 *   TrendingScore = (weightOverall × overallCount) + (weightRecent × recentCount)
 * </pre>
 * Default weights are 0.3 overall and 0.7 recent, but they are configurable.
 *
 * <p>This class contains no Spring annotations and is purely framework-agnostic
 * (DESIGN.md §13.1) so it can be easily tested with varying weights.
 */
public class SuggestionRankingService {

    private final double weightOverall;
    private final double weightRecent;

    public SuggestionRankingService(double weightOverall, double weightRecent) {
        this.weightOverall = weightOverall;
        this.weightRecent = weightRecent;
    }

    /**
     * Compute the trending score for a single query record.
     */
    public double calculateTrendingScore(QueryRecord record) {
        if (record == null) {
            return 0.0;
        }
        return (weightOverall * record.getOverallCount()) + (weightRecent * record.getRecentCount());
    }

    /**
     * Sort the given records by their trending score in descending order.
     * Ties are broken by alphabetical order of the query string to be deterministic.
     */
    public void sort(List<QueryRecord> records) {
        if (records == null || records.size() <= 1) {
            return;
        }
        records.sort((r1, r2) -> {
            double score1 = calculateTrendingScore(r1);
            double score2 = calculateTrendingScore(r2);
            int scoreCompare = Double.compare(score2, score1); // descending
            if (scoreCompare != 0) {
                return scoreCompare;
            }
            // Tie-breaker: alphabetical ascending
            return r1.getQuery().compareTo(r2.getQuery());
        });
    }

    /**
     * Sort the list of query records and return the top N records.
     *
     * @param records The collected query records.
     * @param limit   The maximum number of suggestions to return (typically 10).
     * @return A list of at most {@code limit} QueryRecords.
     */
    public List<QueryRecord> topN(List<QueryRecord> records, int limit) {
        if (records == null) {
            return new ArrayList<>();
        }
        List<QueryRecord> sorted = new ArrayList<>(records);
        sort(sorted);
        if (sorted.size() > limit) {
            return sorted.subList(0, limit);
        }
        return sorted;
    }
}
