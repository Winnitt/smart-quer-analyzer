package com.ai.analytics.smart_query.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.athena.AthenaClient;
import software.amazon.awssdk.services.athena.model.*;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AthenaService {

    private final AthenaClient athenaClient;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    public void createTable(String tableName, String csvHeader) {
        String[] columns = csvHeader.split(",");
        StringBuilder columnDef = new StringBuilder();
        for (String col : columns) {
            columnDef.append(col.trim().replaceAll("[^a-zA-Z0-9]", "_")).append(" STRING, ");
        }
        String finalCols = columnDef.substring(0, columnDef.length() - 2);

        String query = String.format(
                "CREATE EXTERNAL TABLE IF NOT EXISTS %s (%s) " +
                        "ROW FORMAT DELIMITED FIELDS TERMINATED BY ',' " +
                        "LOCATION 's3://%s/raw-data/' " +
                        "TBLPROPERTIES ('skip.header.line.count'='1');",
                tableName, finalCols, bucketName
        );

        runAthenaQuery(query);
    }

    private void runAthenaQuery(String query) {
        StartQueryExecutionRequest request = StartQueryExecutionRequest.builder()
                .queryString(query)
                .queryExecutionContext(QueryExecutionContext.builder().database("default").build())
                .resultConfiguration(ResultConfiguration.builder()
                        .outputLocation("s3://" + bucketName + "/results/")
                        .build())
                .build();
        athenaClient.startQueryExecution(request);
    }

    public String executeQuery(String sql) throws InterruptedException {
        StartQueryExecutionRequest startRequest = StartQueryExecutionRequest.builder()
                .queryString(sql)
                .queryExecutionContext(QueryExecutionContext.builder().database("default").build())
                .resultConfiguration(ResultConfiguration.builder()
                        .outputLocation("s3://" + bucketName + "/results/")
                        .build())
                .build();

        String queryExecutionId = athenaClient.startQueryExecution(startRequest).queryExecutionId();

        boolean isFinished = false;
        while (!isFinished) {
            GetQueryExecutionResponse status = athenaClient.getQueryExecution(b -> b.queryExecutionId(queryExecutionId));
            String state = status.queryExecution().status().stateAsString();
            if (state.equals("SUCCEEDED")) {
                isFinished = true;
            } else if (state.equals("FAILED") || state.equals("CANCELLED")) {
                throw new RuntimeException("Athena Query Failed: " + status.queryExecution().status().stateChangeReason());
            } else {
                Thread.sleep(1000);
            }
        }
        return queryExecutionId;
    }

    // --- ADDED THIS METHOD TO FETCH DATA ---
    public List<String> getQueryResults(String queryExecutionId) {
        GetQueryResultsRequest getQueryResultsRequest = GetQueryResultsRequest.builder()
                .queryExecutionId(queryExecutionId)
                .build();

        GetQueryResultsResponse getQueryResultsResponse = athenaClient.getQueryResults(getQueryResultsRequest);

        // This converts the complex Athena rows into a simple List of Strings separated by "|"
        // Example: "Laptop | Electronics | 1200"
        return getQueryResultsResponse.resultSet().rows().stream()
                .map(row -> row.data().stream()
                        .map(datum -> datum.varCharValue() != null ? datum.varCharValue() : "")
                        .collect(Collectors.joining(" | ")))
                .collect(Collectors.toList());
    }
}