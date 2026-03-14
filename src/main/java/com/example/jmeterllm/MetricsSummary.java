package com.example.jmeterllm;

import java.util.*;

/**
 * Holds aggregated metrics derived from JMeter SampleResult data.
 * This is what we serialize to JSON and send to Gemini.
 */
public class MetricsSummary {

    public static class ResponseTimeStats {
        public long min;
        public double avg;
        public long p50;
        public long p90;
        public long p95;
        public long p99;
        public long max;
    }

    public static class LabelStats {
        public String label;
        public long samples;
        public double errorRatePct;
        public double avgRtMs;
    }

    public static class Overall {
        public long samples;
        public double throughputRps;
        public double errorRatePct;
        public ResponseTimeStats responseTimeMs;
    }

    public static class TestMetadata {
        public String name;
        public long durationSeconds;
        public String startTimeIso;
    }

    public TestMetadata testMetadata;
    public Overall overall;
    public List<LabelStats> perLabel;

    public static MetricsSummary fromAggregator(ResultsAggregator agg) {
        MetricsSummary s = new MetricsSummary();
        s.testMetadata = new TestMetadata();
        s.testMetadata.name = agg.getTestName();
        s.testMetadata.durationSeconds = agg.getDurationSeconds();
        s.testMetadata.startTimeIso = agg.getStartTimeIso();

        s.overall = new Overall();
        s.overall.samples = agg.getTotalSamples();
        s.overall.throughputRps = agg.getThroughputRps();
        s.overall.errorRatePct = agg.getErrorRatePct();
        s.overall.responseTimeMs = new ResponseTimeStats();

        s.overall.responseTimeMs.min = agg.getMinResponseTime();
        s.overall.responseTimeMs.avg = agg.getAvgResponseTime();
        s.overall.responseTimeMs.p50 = agg.getPercentile(50.0);
        s.overall.responseTimeMs.p90 = agg.getPercentile(90.0);
        s.overall.responseTimeMs.p95 = agg.getPercentile(95.0);
        s.overall.responseTimeMs.p99 = agg.getPercentile(99.0);
        s.overall.responseTimeMs.max = agg.getMaxResponseTime();

        s.perLabel = new ArrayList<>();
        for (ResultsAggregator.LabelAggregate la : agg.getPerLabelAggregates()) {
            LabelStats ls = new LabelStats();
            ls.label = la.label;
            ls.samples = la.getSampleCount();
            ls.errorRatePct = la.getErrorRatePct();
            ls.avgRtMs = la.getAvgResponseTime();
            s.perLabel.add(ls);
        }
        return s;
    }
}

