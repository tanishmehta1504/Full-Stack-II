package com.example.dashboard.service;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.stereotype.Service;

@Service
public class QueryStatsService {

    private final Statistics statistics;

    public QueryStatsService(EntityManagerFactory entityManagerFactory) {
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        this.statistics = sessionFactory.getStatistics();
        this.statistics.setStatisticsEnabled(true);
    }

    public long currentPreparedStatementCount() {
        return statistics.getPrepareStatementCount();
    }

    public long queriesSince(long startCount) {
        return statistics.getPrepareStatementCount() - startCount;
    }

    public void clear() {
        statistics.clear();
    }
}
