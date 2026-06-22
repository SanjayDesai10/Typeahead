package com.typeahead.repository;

import com.typeahead.model.TrendingActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TrendingActivityRepository extends JpaRepository<TrendingActivity, Long> {

    Optional<TrendingActivity> findByQueryTextAndBucketHour(String queryText, LocalDateTime bucketHour);

    List<TrendingActivity> findByQueryText(String queryText);

    List<TrendingActivity> findByBucketHourBefore(LocalDateTime cutoff);

    void deleteByBucketHourBefore(LocalDateTime cutoff);

    @Query("SELECT DISTINCT t.queryText FROM TrendingActivity t")
    List<String> findDistinctQueryTexts();
}
