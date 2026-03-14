package com.example.jmeterllm;

import org.apache.jmeter.samplers.SampleResult;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Aggregates JMeter SampleResult data in-memory to compute summary statistics.
 */
public class ResultsAggregator {

    public static class LabelAggregate {
        final String label;
        final AtomicLong sampleCount = new AtomicLong();
        final AtomicLong errorCount = new AtomicLong();
        final AtomicLong totalRt = new AtomicLong();

        LabelAggregate(String label) {
            this.label = label;
        }

        void add(SampleResult res) {
            sampleCount.incrementAndGet();
            totalRt.addAndGet(res.getTime());
            if (!res.isSuccessful()) {
                errorCount.incrementAndGet();
            }
        }

        long getSampleCount() {
            return sampleCount.get();
        }

        double getErrorRatePct() {
            long sc = sampleCount.get();
            if (sc == 0) return 0.0;
            return (errorCount.get() * 100.0) / sc;
        }

        double getAvgResponseTime() {
            long sc = sampleCount.get();
            if (sc == 0) return 0.0;
            return totalRt.get() * 1.0 / sc;
        }
    }

    private final AtomicLong totalSamples = new AtomicLong();
    private final AtomicLong totalErrors = new AtomicLong();
    private final AtomicLong totalRt = new AtomicLong();
    private final List<Long> allResponseTimes = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, LabelAggregate> perLabel = new ConcurrentHashMap<>();

    private long startMillis;
    private volatile long endMillis;
    private volatile String testName = "JMeter Test";

    public ResultsAggregator() {
        this.startMillis = System.currentTimeMillis();
        this.endMillis = this.startMillis;
    }

    /**
     * Adds a sample from explicit values (used for JTL parsing).
     */
    public void addFromValues(String label, long elapsedMillis, boolean success, long timestampMillis) {
        totalSamples.incrementAndGet();
        totalRt.addAndGet(elapsedMillis);
        allResponseTimes.add(elapsedMillis);
        if (!success) {
            totalErrors.incrementAndGet();
        }
        perLabel.computeIfAbsent(label, LabelAggregate::new)
                .add(createSyntheticSample(elapsedMillis, success));

        if (totalSamples.get() == 1) {
            startMillis = timestampMillis;
        }
        if (timestampMillis > endMillis) {
            endMillis = timestampMillis;
        }
    }

    private SampleResult createSyntheticSample(long elapsedMillis, boolean success) {
        SampleResult res = new SampleResult();
        res.setSuccessful(success);
        res.setStampAndTime(System.currentTimeMillis(), elapsedMillis);
        return res;
    }

    public void setTestName(String name) {
        this.testName = name;
    }

    public String getTestName() {
        return testName;
    }

    public void add(SampleResult res) {
        totalSamples.incrementAndGet();
        totalRt.addAndGet(res.getTime());
        allResponseTimes.add(res.getTime());
        if (!res.isSuccessful()) {
            totalErrors.incrementAndGet();
        }
        String label = res.getSampleLabel();
        perLabel.computeIfAbsent(label, LabelAggregate::new).add(res);
        endMillis = System.currentTimeMillis();
    }

    public void clear() {
        totalSamples.set(0);
        totalErrors.set(0);
        totalRt.set(0);
        allResponseTimes.clear();
        perLabel.clear();
        endMillis = System.currentTimeMillis();
    }

    public long getTotalSamples() {
        return totalSamples.get();
    }

    public double getErrorRatePct() {
        long ts = totalSamples.get();
        if (ts == 0) return 0.0;
        return (totalErrors.get() * 100.0) / ts;
    }

    public double getAvgResponseTime() {
        long ts = totalSamples.get();
        if (ts == 0) return 0.0;
        return totalRt.get() * 1.0 / ts;
    }

    public long getMinResponseTime() {
        synchronized (allResponseTimes) {
            return allResponseTimes.stream().mapToLong(Long::longValue).min().orElse(0L);
        }
    }

    public long getMaxResponseTime() {
        synchronized (allResponseTimes) {
            return allResponseTimes.stream().mapToLong(Long::longValue).max().orElse(0L);
        }
    }

    public long getPercentile(double pct) {
        synchronized (allResponseTimes) {
            if (allResponseTimes.isEmpty()) {
                return 0L;
            }
            List<Long> copy = new ArrayList<>(allResponseTimes);
            Collections.sort(copy);
            int index = (int) Math.ceil(pct / 100.0 * copy.size()) - 1;
            if (index < 0) index = 0;
            if (index >= copy.size()) index = copy.size() - 1;
            return copy.get(index);
        }
    }

    public double getThroughputRps() {
        long durMillis = endMillis - startMillis;
        if (durMillis <= 0) return 0.0;
        return (totalSamples.get() * 1000.0) / durMillis;
    }

    public long getDurationSeconds() {
        long durMillis = endMillis - startMillis;
        if (durMillis <= 0) return 0L;
        return durMillis / 1000;
    }

    public String getStartTimeIso() {
        Instant inst = Instant.ofEpochMilli(startMillis);
        return DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC).format(inst);
    }

    public List<LabelAggregate> getPerLabelAggregates() {
        return new ArrayList<>(perLabel.values());
    }
}

