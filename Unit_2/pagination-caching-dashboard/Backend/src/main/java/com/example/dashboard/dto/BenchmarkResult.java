package com.example.dashboard.dto;

import java.io.Serializable;

public class BenchmarkResult implements Serializable {
    private String mode;          // "naive" or "optimized"
    private long queryCount;      // actual SQL statements executed (Hibernate statistics)
    private long timeMillis;      // wall-clock time
    private int recordsReturned;
    private boolean cacheHit;

    public BenchmarkResult() {}

    public BenchmarkResult(String mode, long queryCount, long timeMillis, int recordsReturned, boolean cacheHit) {
        this.mode = mode;
        this.queryCount = queryCount;
        this.timeMillis = timeMillis;
        this.recordsReturned = recordsReturned;
        this.cacheHit = cacheHit;
    }

    public String getMode() { return mode; }
    public long getQueryCount() { return queryCount; }
    public long getTimeMillis() { return timeMillis; }
    public int getRecordsReturned() { return recordsReturned; }
    public boolean isCacheHit() { return cacheHit; }
}
