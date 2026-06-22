package com.typeahead.config;

import com.typeahead.cache.CacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Seeds the database with search queries from queries.csv.
 * Performs fast batch inserts using JdbcTemplate.
 */
@Configuration
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final JdbcTemplate jdbcTemplate;
    private final CacheManager cacheManager;

    @Value("classpath:data/queries.csv")
    private Resource queriesCsvResource;

    public DataSeeder(JdbcTemplate jdbcTemplate, CacheManager cacheManager) {
        this.jdbcTemplate = jdbcTemplate;
        this.cacheManager = cacheManager;
    }

    @Bean
    public CommandLineRunner seedDatabase() {
        return args -> {
            Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM queries", Long.class);
            long dbCount = count != null ? count : 0;

            // Seed if the database is empty or if it was seeded with the old synthetic data (105001 queries)
            if (dbCount > 0 && dbCount != 105001) {
                log.info("Database already contains {} queries. Skipping CSV seeding.", dbCount);
                return;
            }

            log.info("Starting database seeding from queries.csv. Current db count: {}", dbCount);
            long startTime = System.currentTimeMillis();

            // 1. Truncate existing tables to avoid duplicate key issues and start clean
            log.info("Truncating tables: queries, trending_activity...");
            jdbcTemplate.execute("TRUNCATE TABLE queries RESTART IDENTITY CASCADE");
            jdbcTemplate.execute("TRUNCATE TABLE trending_activity RESTART IDENTITY CASCADE");

            // 2. Clear Redis caches across all nodes
            log.info("Flushing Redis cache...");
            cacheManager.flushAllCaches();

            // 3. Read CSV and insert queries in batches
            String sql = "INSERT INTO queries (query_text, count, trending_score, last_searched_at, created_at) " +
                    "VALUES (?, ?, ?, ?, ?) " +
                    "ON CONFLICT (query_text) DO NOTHING";

            Random random = new Random(42);
            int batchSize = 5000;
            List<Object[]> batchArgs = new ArrayList<>(batchSize);

            long totalLoaded = 0;
            long linesProcessed = 0;

            try (BufferedReader br = new BufferedReader(new InputStreamReader(queriesCsvResource.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                // Skip header: "query,count"
                String header = br.readLine();
                if (header == null) {
                    throw new IllegalStateException("CSV file is empty!");
                }

                while ((line = br.readLine()) != null) {
                    linesProcessed++;
                    int lastComma = line.lastIndexOf(',');
                    if (lastComma == -1) {
                        log.warn("Skipping malformed line {}: {}", linesProcessed + 1, line);
                        continue;
                    }

                    String queryText = line.substring(0, lastComma).trim();
                    String countStr = line.substring(lastComma + 1).trim();

                    if (queryText.isEmpty()) {
                        continue;
                    }

                    long qCount;
                    try {
                        qCount = Long.parseLong(countStr);
                    } catch (NumberFormatException e) {
                        log.warn("Skipping line {}: count '{}' is not a number", linesProcessed + 1, countStr);
                        continue;
                    }

                    // Generate random dates & trending score mimicking the original synthetic behavior
                    double trendingScore = qCount * (0.3 + random.nextDouble() * 0.7);
                    LocalDateTime lastSearchedAt = LocalDateTime.now().minusHours(random.nextInt(48));
                    LocalDateTime createdAt = LocalDateTime.now().minusDays(random.nextInt(90));

                    batchArgs.add(new Object[]{
                            queryText,
                            qCount,
                            trendingScore,
                            Timestamp.valueOf(lastSearchedAt),
                            Timestamp.valueOf(createdAt)
                    });

                    if (batchArgs.size() >= batchSize) {
                        totalLoaded += executeBatch(sql, batchArgs);
                        batchArgs.clear();
                        if (totalLoaded % 50000 == 0 || totalLoaded % 50000 < batchSize) {
                            log.info("Seeded {} queries...", totalLoaded);
                        }
                    }
                }

                // Insert remaining
                if (!batchArgs.isEmpty()) {
                    totalLoaded += executeBatch(sql, batchArgs);
                    batchArgs.clear();
                }

            } catch (Exception e) {
                log.error("Error seeding database from CSV: {}", e.getMessage(), e);
                throw new RuntimeException(e);
            }

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("Database seeding complete: Loaded {}/{} queries from CSV in {}ms", totalLoaded, linesProcessed, elapsed);
        };
    }

    private int executeBatch(String sql, List<Object[]> batchArgs) {
        int[] results = jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Object[] args = batchArgs.get(i);
                ps.setString(1, (String) args[0]);
                ps.setLong(2, (Long) args[1]);
                ps.setDouble(3, (Double) args[2]);
                ps.setTimestamp(4, (Timestamp) args[3]);
                ps.setTimestamp(5, (Timestamp) args[4]);
            }

            @Override
            public int getBatchSize() {
                return batchArgs.size();
            }
        });

        int successCount = 0;
        for (int res : results) {
            if (res > 0 || res == -2) {
                successCount++;
            }
        }
        return successCount;
    }
}
