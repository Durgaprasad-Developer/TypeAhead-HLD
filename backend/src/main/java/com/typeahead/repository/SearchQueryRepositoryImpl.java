package com.typeahead.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.hibernate.Session;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Custom implementation for {@link SearchQueryRepositoryCustom}.
 *
 * <p>Detects the database engine (H2 vs PostgreSQL) and executes the appropriate
 * upsert mechanism (PostgreSQL native ON CONFLICT vs H2 select-then-write fallback).
 */
@Repository
public class SearchQueryRepositoryImpl implements SearchQueryRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public int upsertQueryCount(String query, long count, Instant now) {
        Session session = entityManager.unwrap(Session.class);
        String databaseProductName = session.doReturningWork(conn -> conn.getMetaData().getDatabaseProductName());

        if ("H2".equalsIgnoreCase(databaseProductName)) {
            // H2 Fallback: select then insert/update to bypass lack of ON CONFLICT support
            Query selectQuery = entityManager.createNativeQuery("SELECT id, overall_count, recent_count FROM search_queries WHERE query = ?");
            selectQuery.setParameter(1, query);
            List<?> results = selectQuery.getResultList();

            if (results.isEmpty()) {
                // Insert new query
                Query insertQuery = entityManager.createNativeQuery(
                    "INSERT INTO search_queries (query, overall_count, recent_count, last_searched, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?)"
                );
                insertQuery.setParameter(1, query);
                insertQuery.setParameter(2, count);
                insertQuery.setParameter(3, count);
                insertQuery.setParameter(4, now);
                insertQuery.setParameter(5, now);
                insertQuery.setParameter(6, now);
                return insertQuery.executeUpdate();
            } else {
                // Update existing query
                Object[] row = (Object[]) results.get(0);
                Number id = (Number) row[0];
                Number overall = (Number) row[1];
                Number recent = (Number) row[2];

                Query updateQuery = entityManager.createNativeQuery(
                    "UPDATE search_queries SET overall_count = ?, recent_count = ?, last_searched = ?, updated_at = ? WHERE id = ?"
                );
                updateQuery.setParameter(1, overall.longValue() + count);
                updateQuery.setParameter(2, recent.longValue() + count);
                updateQuery.setParameter(3, now);
                updateQuery.setParameter(4, now);
                updateQuery.setParameter(5, id.longValue());
                return updateQuery.executeUpdate();
            }
        } else {
            // PostgreSQL high-performance native ON CONFLICT upsert
            Query pgQuery = entityManager.createNativeQuery(
                "INSERT INTO search_queries (query, overall_count, recent_count, last_searched, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT (query) DO UPDATE " +
                "SET overall_count = search_queries.overall_count + EXCLUDED.overall_count, " +
                "    recent_count = search_queries.recent_count + EXCLUDED.recent_count, " +
                "    last_searched = EXCLUDED.last_searched, " +
                "    updated_at = EXCLUDED.updated_at"
            );
            pgQuery.setParameter(1, query);
            pgQuery.setParameter(2, count);
            pgQuery.setParameter(3, count);
            pgQuery.setParameter(4, now);
            pgQuery.setParameter(5, now);
            pgQuery.setParameter(6, now);
            return pgQuery.executeUpdate();
        }
    }
}
