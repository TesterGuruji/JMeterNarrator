package com.example.jmeterllm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Very simple CSV JTL parser that loads key metrics into a ResultsAggregator.
 *
 * It expects a header row with at least:
 * - timeStamp
 * - elapsed
 * - success
 * - label
 *
 * and parses subsequent rows as comma-separated values without embedded commas.
 */
public final class JtlCsvLoader {

    private JtlCsvLoader() {
    }

    public static ResultsAggregator load(File file) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("JTL file is null");
        }
        if (!file.exists() || !file.isFile()) {
            throw new IOException("JTL file does not exist: " + file.getAbsolutePath());
        }

        ResultsAggregator agg = new ResultsAggregator();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IOException("JTL file is empty: " + file.getAbsolutePath());
            }

            String[] headers = headerLine.split(",");
            Map<String, Integer> indexByName = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                indexByName.put(headers[i].trim(), i);
            }

            Integer idxTs = indexByName.get("timeStamp");
            Integer idxElapsed = indexByName.get("elapsed");
            Integer idxSuccess = indexByName.get("success");
            Integer idxLabel = indexByName.get("label");

            if (idxTs == null || idxElapsed == null || idxSuccess == null || idxLabel == null) {
                throw new IOException("JTL header must contain timeStamp, elapsed, success, label columns.");
            }

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) {
                    continue;
                }
                String[] parts = line.split(",");
                if (parts.length <= Math.max(Math.max(idxTs, idxElapsed), Math.max(idxSuccess, idxLabel))) {
                    continue;
                }

                try {
                    long ts = Long.parseLong(parts[idxTs].trim());
                    long elapsed = Long.parseLong(parts[idxElapsed].trim());
                    boolean success = Boolean.parseBoolean(parts[idxSuccess].trim());
                    String label = parts[idxLabel].trim();

                    agg.addFromValues(label, elapsed, success, ts);
                } catch (NumberFormatException nfe) {
                    // skip malformed line
                }
            }
        }

        return agg;
    }
}

