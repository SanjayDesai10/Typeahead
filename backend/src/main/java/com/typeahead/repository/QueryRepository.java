package com.typeahead.repository;

import com.typeahead.model.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface QueryRepository extends JpaRepository<Query, Long> {

    /**
     * Find top suggestions matching a prefix, sorted by count descending.
     */
    List<Query> findTop10ByQueryTextStartingWithIgnoreCaseOrderByCountDesc(String prefix);

    /**
     * Find top suggestions matching a prefix, sorted by trending score descending.
     */
    List<Query> findTop10ByQueryTextStartingWithIgnoreCaseOrderByTrendingScoreDesc(String prefix);

    /**
     * Find a query by its exact text (case-insensitive).
     */
    Optional<Query> findByQueryTextIgnoreCase(String queryText);

    /**
     * Get top trending queries overall.
     */
    List<Query> findTop10ByOrderByTrendingScoreDesc();

    /**
     * Count total queries in the database.
     */
    long count();
}
