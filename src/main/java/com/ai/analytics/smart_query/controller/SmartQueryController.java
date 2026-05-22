package com.ai.analytics.smart_query.controller;

import com.ai.analytics.smart_query.model.TableMetadata;
import com.ai.analytics.smart_query.repository.QueryHistoryRepository;
import com.ai.analytics.smart_query.repository.TableMetadataRepository;
import com.ai.analytics.smart_query.service.AIService;
import com.ai.analytics.smart_query.service.AthenaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")

public class SmartQueryController {

    private final AIService aiService;
    private final AthenaService athenaService;

    // NEW: Inject the repository to look up column names dynamically
    private final TableMetadataRepository tableMetadataRepository;
    private final QueryHistoryRepository historyRepository;

    @GetMapping("/ask")
    public Map<String, Object> ask(@RequestParam String question, @RequestParam String table) {
        try {
            TableMetadata meta = tableMetadataRepository.findByTableName(table)
                    .orElseThrow(() -> new RuntimeException("Table not found"));

            // 1. Call AI and get the Map
            Map<String, String> aiResult = aiService.translateToSQL(question, table, meta.getColumns());

            // 2. Extract SQL and clean it
            String sql = aiResult.get("sql").replace(";", "").trim();

            // 3. Run Query on Athena
            String executionId = athenaService.executeQuery(sql);
            List<String> data = athenaService.getQueryResults(executionId);

            // 4. Return everything to the React UI
            Map<String, Object> finalResponse = new HashMap<>();
            finalResponse.put("sql", sql);
            finalResponse.put("explanation", aiResult.get("explanation"));
            finalResponse.put("chartSuggestion", aiResult.get("chartSuggestion"));
            finalResponse.put("data", data);

            return finalResponse;
        } catch (Exception e) {
            Map<String, Object> errorRes = new HashMap<>();
            errorRes.put("error", e.getMessage());
            return errorRes;
        }
    }
}