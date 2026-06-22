package com.typeahead.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "queries", indexes = {
    @Index(name = "idx_query_text", columnList = "queryText"),
    @Index(name = "idx_count_desc", columnList = "count DESC"),
    @Index(name = "idx_trending_score", columnList = "trendingScore DESC")
})
public class Query {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "query_text", unique = true, nullable = false, length = 500)
    private String queryText;

    @Column(name = "count", nullable = false)
    private Long count = 0L;

    @Column(name = "trending_score")
    private Double trendingScore = 0.0;

    @Column(name = "last_searched_at")
    private LocalDateTime lastSearchedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public Query() {}

    public Query(Long id, String queryText, Long count, Double trendingScore,
                 LocalDateTime lastSearchedAt, LocalDateTime createdAt) {
        this.id = id;
        this.queryText = queryText;
        this.count = count;
        this.trendingScore = trendingScore;
        this.lastSearchedAt = lastSearchedAt;
        this.createdAt = createdAt;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getQueryText() { return queryText; }
    public void setQueryText(String queryText) { this.queryText = queryText; }

    public Long getCount() { return count; }
    public void setCount(Long count) { this.count = count; }

    public Double getTrendingScore() { return trendingScore; }
    public void setTrendingScore(Double trendingScore) { this.trendingScore = trendingScore; }

    public LocalDateTime getLastSearchedAt() { return lastSearchedAt; }
    public void setLastSearchedAt(LocalDateTime lastSearchedAt) { this.lastSearchedAt = lastSearchedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // Builder pattern
    public static QueryBuilder builder() { return new QueryBuilder(); }

    public static class QueryBuilder {
        private Long id;
        private String queryText;
        private Long count = 0L;
        private Double trendingScore = 0.0;
        private LocalDateTime lastSearchedAt;
        private LocalDateTime createdAt = LocalDateTime.now();

        public QueryBuilder id(Long id) { this.id = id; return this; }
        public QueryBuilder queryText(String queryText) { this.queryText = queryText; return this; }
        public QueryBuilder count(Long count) { this.count = count; return this; }
        public QueryBuilder trendingScore(Double trendingScore) { this.trendingScore = trendingScore; return this; }
        public QueryBuilder lastSearchedAt(LocalDateTime lastSearchedAt) { this.lastSearchedAt = lastSearchedAt; return this; }
        public QueryBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public Query build() {
            return new Query(id, queryText, count, trendingScore, lastSearchedAt, createdAt);
        }
    }
}
