package com.example.jmeterllm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.jmeter.util.JMeterUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Simple HTTP client for calling Google's Gemini API.
 */
public class GeminiClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String apiKey;
    private final String model;

    public GeminiClient(String apiKey, String model) {
        this.apiKey = apiKey;
        this.model = model;
    }

    public static String resolveApiKey() {
        String key = System.getenv("GEMINI_API_KEY");
        if (key == null || key.isEmpty()) {
            key = JMeterUtils.getProperty("gemini.api.key");
        }
        return key;
    }

    public String analyze(String jsonSummary) throws IOException {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IOException("Gemini API key not configured. Set GEMINI_API_KEY env or gemini.api.key JMeter property.");
        }

        String endpoint = String.format(
                "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s",
                model,
                URLEncoder.encode(apiKey, StandardCharsets.UTF_8)
        );

        String promptText = buildPrompt(jsonSummary);

        // Request body according to Gemini REST API:
        // {
        //   "contents": [{
        //     "role": "user",
        //     "parts": [{ "text": "..." }]
        //   }]
        // }
        ObjectNode root = MAPPER.createObjectNode();
        ArrayNode contents = root.putArray("contents");
        ObjectNode userContent = contents.addObject();
        userContent.put("role", "user");
        ArrayNode parts = userContent.putArray("parts");
        ObjectNode textPart = parts.addObject();
        textPart.put("text", promptText);

        String requestJson = root.toString();

        HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");

        try (OutputStream os = conn.getOutputStream()) {
            os.write(requestJson.getBytes(StandardCharsets.UTF_8));
        }

        int status = conn.getResponseCode();
        String responseBody = new String(
                (status >= 200 && status < 300 ? conn.getInputStream() : conn.getErrorStream())
                        .readAllBytes(),
                StandardCharsets.UTF_8
        );
        conn.disconnect();

        if (status < 200 || status >= 300) {
            throw new IOException("Gemini API error (" + status + "): " + responseBody);
        }

        return extractText(responseBody);
    }

    private String buildPrompt(String jsonSummary) {
        return "You are a performance engineering expert. "
                + "Analyze the following JMeter test run metrics and return Markdown.\n\n"
                + jsonSummary + "\n\n"
                + "Please include:\n"
                + "1. High-level summary of performance and stability.\n"
                + "2. Key observations, including slow or error-prone endpoints.\n"
                + "3. Concrete recommendations for tuning, scaling, or further testing.\n"
                + "4. Any suspected root causes based on the metrics.\n";
    }

    private String extractText(String responseJson) throws IOException {
        JsonNode root = MAPPER.readTree(responseJson);
        JsonNode candidates = root.path("candidates");
        if (!candidates.isArray() || candidates.isEmpty()) {
            return "No analysis returned by Gemini.";
        }
        JsonNode content = candidates.get(0).path("content");
        JsonNode parts = content.path("parts");
        if (!parts.isArray() || parts.isEmpty()) {
            return "No text parts returned by Gemini.";
        }
        StringBuilder sb = new StringBuilder();
        for (JsonNode part : parts) {
            if (part.has("text")) {
                sb.append(part.get("text").asText());
            }
        }
        if (sb.length() == 0) {
            return "Empty text returned by Gemini.";
        }
        return sb.toString();
    }
}

