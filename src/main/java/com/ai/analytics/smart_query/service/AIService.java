package com.ai.analytics.smart_query.service;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

@Service
public class AIService {

    @Value("${ai.api.key}")
    private String apiKey;

    // We change return type to Map<String, String> so the Controller can see sql, explanation, etc.
    public Map<String, String> translateToSQL(String question, String tableName, String columns) throws Exception {
        String prompt = String.format(
                "You are an AI Data Analyst. Return ONLY a JSON object with these keys: " +
                        "'sql' (the Athena SQL query), " +
                        "'explanation' (a brief insight about what this data shows), " +
                        "'chartSuggestion' (BAR, PIE, or NONE). " +
                        "--- DATA CONTEXT --- " +
                        "Table: '%s', Columns: [%s]. " +
                        "--- RULES --- " +
                        "1. Use CAST(column AS DOUBLE) for all math. " +
                        "2. Do NOT use markdown. Do NOT use backticks. " +
                        "3. Task: Convert '%s' to SQL.",
                tableName, columns, question
        );

        JSONObject payload = new JSONObject();
        payload.put("model", "llama-3.3-70b-versatile");
        JSONArray messages = new JSONArray();
        messages.put(new JSONObject().put("role", "user").put("content", prompt));
        payload.put("messages", messages);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.groq.com/openai/v1/chat/completions"))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

        // 1. Get the content from the AI response
        JSONObject fullRes = new JSONObject(response.body());
        String content = fullRes.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content").trim();

        // 2. Cleaning: AI sometimes adds ```json ... ``` backticks even when told not to
        content = content.replace("```json", "").replace("```", "").trim();

        // 3. Parse the AI's JSON message
        JSONObject aiJson = new JSONObject(content);

        // 4. Wrap it in a Map to return to the Controller
        Map<String, String> resultMap = new HashMap<>();
        resultMap.put("sql", aiJson.getString("sql"));
        resultMap.put("explanation", aiJson.getString("explanation"));
        resultMap.put("chartSuggestion", aiJson.getString("chartSuggestion"));

        return resultMap;
    }
}