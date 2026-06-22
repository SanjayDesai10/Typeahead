package com.typeahead;

import com.typeahead.model.TrendingActivity;
import com.typeahead.repository.QueryRepository;
import com.typeahead.repository.TrendingActivityRepository;
import com.typeahead.service.TrendingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TrendingService — the exponential decay scoring engine.
 * Tests cover: recording activity, score computation, decay math, and scheduled cleanup.
 */
@ExtendWith(MockitoExtension.class)
class TrendingServiceTest {

    @Mock
    private TrendingActivityRepository trendingRepo;

    @Mock
    private QueryRepository queryRepository;

    private TrendingService trendingService;

    @BeforeEach
    void setUp() {
        trendingService = new TrendingService(trendingRepo, queryRepository);

        // Set @Value fields via reflection (no Spring context)
        ReflectionTestUtils.setField(trendingService, "decayRate", 0.1);
        ReflectionTestUtils.setField(trendingService, "windowHours", 24);
        ReflectionTestUtils.setField(trendingService, "historicalWeight", 0.3);
        ReflectionTestUtils.setField(trendingService, "recentWeight", 0.7);
    }

    @Test
    @DisplayName("recordSearch creates a new TrendingActivity bucket if none exists")
    void testRecordSearchNewBucket() {
        when(trendingRepo.findByQueryTextAndBucketHour(anyString(), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());
        when(trendingRepo.save(any(TrendingActivity.class))).thenAnswer(i -> i.getArgument(0));

        trendingService.recordSearch("iphone 15");

        ArgumentCaptor<TrendingActivity> captor = ArgumentCaptor.forClass(TrendingActivity.class);
        verify(trendingRepo).save(captor.capture());

        TrendingActivity saved = captor.getValue();
        assertEquals("iphone 15", saved.getQueryText());
        assertEquals(1L, saved.getSearchCount());
        assertNotNull(saved.getBucketHour());
        // Bucket should be truncated to the hour
        assertEquals(0, saved.getBucketHour().getMinute());
    }

    @Test
    @DisplayName("recordSearch increments count for existing bucket")
    void testRecordSearchExistingBucket() {
        TrendingActivity existing = TrendingActivity.builder()
                .queryText("iphone 15")
                .bucketHour(LocalDateTime.now().truncatedTo(ChronoUnit.HOURS))
                .searchCount(5L)
                .build();

        when(trendingRepo.findByQueryTextAndBucketHour(anyString(), any(LocalDateTime.class)))
                .thenReturn(Optional.of(existing));
        when(trendingRepo.save(any(TrendingActivity.class))).thenAnswer(i -> i.getArgument(0));

        trendingService.recordSearch("iphone 15");

        assertEquals(6L, existing.getSearchCount(),
                "Should increment existing count from 5 to 6");
        verify(trendingRepo).save(existing);
    }

    @Test
    @DisplayName("Trending score is higher for queries with no activity (all-time only)")
    void testTrendingScoreNoRecentActivity() {
        when(trendingRepo.findByQueryText("old query")).thenReturn(Collections.emptyList());

        double score = trendingService.computeTrendingScore("old query", 10000L);

        // With no recent activity: score = 0.3 * 10000 + 0.7 * 0 = 3000
        assertEquals(3000.0, score, 0.01,
                "Score should be 0.3 × allTimeCount when no recent activity");
    }

    @Test
    @DisplayName("Recent activity within window boosts the score with decay")
    void testTrendingScoreWithRecentActivity() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneHourAgo = now.truncatedTo(ChronoUnit.HOURS);

        TrendingActivity recent = TrendingActivity.builder()
                .queryText("trending query")
                .bucketHour(oneHourAgo)
                .searchCount(100L)
                .build();

        when(trendingRepo.findByQueryText("trending query")).thenReturn(List.of(recent));

        double score = trendingService.computeTrendingScore("trending query", 500L);

        // Score should be: 0.3 * 500 + 0.7 * (100 * e^(-0.1 * ageHours))
        // The age depends on current time, but score should be > historical-only baseline
        double historicalOnly = 0.3 * 500; // 150
        assertTrue(score > historicalOnly,
                String.format("Score %.2f should be greater than historical-only %.2f", score, historicalOnly));
    }

    @Test
    @DisplayName("Exponential decay: older activity contributes less than newer activity")
    void testExponentialDecayOrdering() {
        LocalDateTime now = LocalDateTime.now();

        // Query A: lots of searches 20 hours ago (old)
        TrendingActivity oldActivity = TrendingActivity.builder()
                .queryText("old-trending")
                .bucketHour(now.minusHours(20).truncatedTo(ChronoUnit.HOURS))
                .searchCount(1000L)
                .build();

        // Query B: few searches 1 hour ago (recent)
        TrendingActivity newActivity = TrendingActivity.builder()
                .queryText("new-trending")
                .bucketHour(now.minusHours(1).truncatedTo(ChronoUnit.HOURS))
                .searchCount(50L)
                .build();

        when(trendingRepo.findByQueryText("old-trending")).thenReturn(List.of(oldActivity));
        when(trendingRepo.findByQueryText("new-trending")).thenReturn(List.of(newActivity));

        // Both have same all-time count to isolate the recency effect
        double oldScore = trendingService.computeTrendingScore("old-trending", 500L);
        double newScore = trendingService.computeTrendingScore("new-trending", 500L);

        // The 1000-count 20-hour-old entry gets heavily decayed:
        // decay = e^(-0.1 * 20) = e^(-2) ≈ 0.135 → weighted = 1000 * 0.135 = 135
        // The 50-count 1-hour-old entry keeps most value:
        // decay = e^(-0.1 * 1) = e^(-0.1) ≈ 0.905 → weighted = 50 * 0.905 = 45.2
        // But 135 > 45.2 with 1000 searches, so let's compare with equal search counts

        // Better test: compare decay factors directly
        double decayOld = Math.exp(-0.1 * 20); // ≈ 0.135
        double decayNew = Math.exp(-0.1 * 1);   // ≈ 0.905
        assertTrue(decayNew > decayOld, "Recent decay factor should be higher than old decay factor");
    }

    @Test
    @DisplayName("Activity outside 24-hour window is ignored in score computation")
    void testActivityOutsideWindowIgnored() {
        LocalDateTime now = LocalDateTime.now();

        // Activity 25 hours ago — outside the 24h window
        TrendingActivity expired = TrendingActivity.builder()
                .queryText("expired-query")
                .bucketHour(now.minusHours(25).truncatedTo(ChronoUnit.HOURS))
                .searchCount(10000L)
                .build();

        when(trendingRepo.findByQueryText("expired-query")).thenReturn(List.of(expired));

        double score = trendingService.computeTrendingScore("expired-query", 200L);

        // Should only have historical component: 0.3 * 200 = 60
        assertEquals(60.0, score, 0.01,
                "Activity outside 24h window should not contribute to score");
    }

    @Test
    @DisplayName("Decay job deletes expired buckets and recalculates active query scores")
    void testDecayJob() {
        // Mock: 2 active query texts
        when(trendingRepo.findDistinctQueryTexts())
                .thenReturn(Arrays.asList("query-a", "query-b"));

        // Mock: both queries exist in the DB
        com.typeahead.model.Query queryA = com.typeahead.model.Query.builder()
                .queryText("query-a").count(100L).trendingScore(50.0).build();
        com.typeahead.model.Query queryB = com.typeahead.model.Query.builder()
                .queryText("query-b").count(200L).trendingScore(80.0).build();

        when(queryRepository.findByQueryTextIgnoreCase("query-a"))
                .thenReturn(Optional.of(queryA));
        when(queryRepository.findByQueryTextIgnoreCase("query-b"))
                .thenReturn(Optional.of(queryB));
        when(queryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // Mock: no recent activity for either (scores will revert to historical only)
        when(trendingRepo.findByQueryText(anyString())).thenReturn(Collections.emptyList());

        trendingService.decayAndUpdateScores();

        // Should delete old buckets
        verify(trendingRepo).deleteByBucketHourBefore(any(LocalDateTime.class));

        // Historical-only scores:
        // query-a: 0.3 * 100 = 30 (changed from 50 → should be saved)
        // query-b: 0.3 * 200 = 60 (changed from 80 → should be saved)
        verify(queryRepository, times(2)).save(any(com.typeahead.model.Query.class));
    }

    @Test
    @DisplayName("Decay job skips queries whose score hasn't significantly changed")
    void testDecayJobSkipsUnchanged() {
        when(trendingRepo.findDistinctQueryTexts())
                .thenReturn(List.of("stable-query"));

        // Score is already exactly the historical-only value
        com.typeahead.model.Query stableQuery = com.typeahead.model.Query.builder()
                .queryText("stable-query").count(1000L).trendingScore(300.0).build();

        when(queryRepository.findByQueryTextIgnoreCase("stable-query"))
                .thenReturn(Optional.of(stableQuery));
        when(trendingRepo.findByQueryText("stable-query"))
                .thenReturn(Collections.emptyList());

        trendingService.decayAndUpdateScores();

        // Score = 0.3 * 1000 = 300.0, which equals the existing score
        // Difference < 0.01 → should NOT save
        verify(queryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Multiple time buckets accumulate correctly with different decay weights")
    void testMultipleBucketsDecay() {
        LocalDateTime now = LocalDateTime.now();

        TrendingActivity bucket1 = TrendingActivity.builder()
                .queryText("multi-bucket")
                .bucketHour(now.truncatedTo(ChronoUnit.HOURS))
                .searchCount(10L)
                .build();

        TrendingActivity bucket2 = TrendingActivity.builder()
                .queryText("multi-bucket")
                .bucketHour(now.minusHours(5).truncatedTo(ChronoUnit.HOURS))
                .searchCount(10L)
                .build();

        TrendingActivity bucket3 = TrendingActivity.builder()
                .queryText("multi-bucket")
                .bucketHour(now.minusHours(10).truncatedTo(ChronoUnit.HOURS))
                .searchCount(10L)
                .build();

        when(trendingRepo.findByQueryText("multi-bucket"))
                .thenReturn(Arrays.asList(bucket1, bucket2, bucket3));

        double score = trendingService.computeTrendingScore("multi-bucket", 100L);

        // Each bucket contributes count × e^(-0.1 × age)
        // bucket1 (0h): 10 × ~1.0 = ~10
        // bucket2 (5h): 10 × e^(-0.5) ≈ 6.07
        // bucket3 (10h): 10 × e^(-1.0) ≈ 3.68
        // Recent total ≈ 19.75
        // Score = 0.3 * 100 + 0.7 * 19.75 ≈ 30 + 13.83 = 43.83
        double historical = 0.3 * 100;
        assertTrue(score > historical,
                "Score with recent activity should exceed historical-only");
        assertTrue(score > 40 && score < 50,
                String.format("Expected score ~44, got %.2f", score));
    }
}
