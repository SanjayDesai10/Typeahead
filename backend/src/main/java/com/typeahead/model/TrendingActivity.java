package com.typeahead.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "trending_activity", indexes = {
    @Index(name = "idx_ta_query_bucket", columnList = "queryText, bucketHour")
})
public class TrendingActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "query_text", nullable = false, length = 500)
    private String queryText;

    @Column(name = "bucket_hour", nullable = false)
    private LocalDateTime bucketHour;

    @Column(name = "search_count", nullable = false)
    private Long searchCount = 0L;

    public TrendingActivity() {}

    public TrendingActivity(Long id, String queryText, LocalDateTime bucketHour, Long searchCount) {
        this.id = id;
        this.queryText = queryText;
        this.bucketHour = bucketHour;
        this.searchCount = searchCount;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getQueryText() { return queryText; }
    public void setQueryText(String queryText) { this.queryText = queryText; }

    public LocalDateTime getBucketHour() { return bucketHour; }
    public void setBucketHour(LocalDateTime bucketHour) { this.bucketHour = bucketHour; }

    public Long getSearchCount() { return searchCount; }
    public void setSearchCount(Long searchCount) { this.searchCount = searchCount; }

    public static TrendingActivityBuilder builder() { return new TrendingActivityBuilder(); }

    public static class TrendingActivityBuilder {
        private Long id;
        private String queryText;
        private LocalDateTime bucketHour;
        private Long searchCount = 0L;

        public TrendingActivityBuilder id(Long id) { this.id = id; return this; }
        public TrendingActivityBuilder queryText(String queryText) { this.queryText = queryText; return this; }
        public TrendingActivityBuilder bucketHour(LocalDateTime bucketHour) { this.bucketHour = bucketHour; return this; }
        public TrendingActivityBuilder searchCount(Long searchCount) { this.searchCount = searchCount; return this; }

        public TrendingActivity build() {
            return new TrendingActivity(id, queryText, bucketHour, searchCount);
        }
    }
}
