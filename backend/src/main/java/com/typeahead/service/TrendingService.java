package com.typeahead.service;

import com.typeahead.model.TrendingActivity;
import com.typeahead.repository.QueryRepository;
import com.typeahead.repository.TrendingActivityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
public class TrendingService {

    private static final Logger log = LoggerFactory.getLogger(TrendingService.class);

    private final TrendingActivityRepository trendingRepo;
    private final QueryRepository queryRepository;

    @Value("${typeahead.trending.decay-rate:0.1}")
    private double decayRate;

    @Value("${typeahead.trending.window-hours:24}")
    private int windowHours;

    @Value("${typeahead.trending.historical-weight:0.3}")
    private double historicalWeight;

    @Value("${typeahead.trending.recent-weight:0.7}")
    private double recentWeight;

    public TrendingService(TrendingActivityRepository trendingRepo,
                           QueryRepository queryRepository) {
        this.trendingRepo = trendingRepo;
        this.queryRepository = queryRepository;
    }

    @Transactional
    public void recordSearch(String queryText) {
        LocalDateTime currentBucket = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);
        Optional<TrendingActivity> existing = trendingRepo.findByQueryTextAndBucketHour(queryText, currentBucket);

        if (existing.isPresent()) {
            TrendingActivity activity = existing.get();
            activity.setSearchCount(activity.getSearchCount() + 1);
            trendingRepo.save(activity);
        } else {
            TrendingActivity activity = TrendingActivity.builder()
                    .queryText(queryText)
                    .bucketHour(currentBucket)
                    .searchCount(1L)
                    .build();
            trendingRepo.save(activity);
        }
    }

    public double computeTrendingScore(String queryText, long allTimeCount) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.minusHours(windowHours);

        List<TrendingActivity> recentActivity = trendingRepo.findByQueryText(queryText);

        double recentWeightedScore = 0.0;
        for (TrendingActivity activity : recentActivity) {
            if (activity.getBucketHour().isAfter(cutoff)) {
                double ageHours = ChronoUnit.MINUTES.between(activity.getBucketHour(), now) / 60.0;
                double decay = Math.exp(-decayRate * ageHours);
                recentWeightedScore += activity.getSearchCount() * decay;
            }
        }

        return historicalWeight * allTimeCount + recentWeight * recentWeightedScore;
    }

    @Scheduled(fixedDelayString = "${typeahead.trending.decay-job-interval-ms:900000}")
    @Transactional
    public void decayAndUpdateScores() {
        log.info("Running trending score decay job...");
        LocalDateTime cutoff = LocalDateTime.now().minusHours(windowHours);

        trendingRepo.deleteByBucketHourBefore(cutoff);

        // Only update scores for queries that have recent trending activity
        // (not all 105K queries — that would be too slow)
        List<String> activeQueryTexts = trendingRepo.findDistinctQueryTexts();
        int updated = 0;

        for (String queryText : activeQueryTexts) {
            queryRepository.findByQueryTextIgnoreCase(queryText).ifPresent(query -> {
                double newScore = computeTrendingScore(query.getQueryText(), query.getCount());
                if (Math.abs(query.getTrendingScore() - newScore) > 0.01) {
                    query.setTrendingScore(newScore);
                    queryRepository.save(query);
                }
            });
            updated++;
        }

        log.info("Trending decay job complete: processed {} active queries, cleaned buckets before {}", updated, cutoff);
    }
}
