package com.ai.analytics.smart_query.service;

import com.ai.analytics.smart_query.model.TableMetadata;
import com.ai.analytics.smart_query.repository.TableMetadataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class StorageService {

    private final S3Client s3Client;
    private final AthenaService athenaService;

    // NEW: Inject the repository to save the table's "Identity"
    private final TableMetadataRepository tableMetadataRepository;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    public String uploadFile(MultipartFile file) throws IOException {
        String originalName = file.getOriginalFilename();
        String tableName = originalName.replaceAll("\\.(csv|xlsx|xls)", "").replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();

        byte[] csvContent;
        if (originalName.endsWith(".xlsx") || originalName.endsWith(".xls")) {
            csvContent = convertExcelToCsv(file); // Professional conversion
        } else {
            csvContent = file.getBytes();
        }

        // Upload processed CSV to S3
        s3Client.putObject(PutObjectRequest.builder()
                .bucket(bucketName)
                .key("raw-data/" + tableName + ".csv")
                .build(), RequestBody.fromBytes(csvContent));

        String header = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(csvContent))).readLine();
        athenaService.createTable(tableName, header);

        // Save to Catalog
        TableMetadata meta = new TableMetadata();
        meta.setTableName(tableName);
        meta.setColumns(header);
        tableMetadataRepository.save(meta);

        return tableName;
    }

    private byte[] convertExcelToCsv(MultipartFile file) throws IOException {
        StringBuilder csv = new StringBuilder();
        try (org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook(file.getInputStream())) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheetAt(0);
            for (org.apache.poi.ss.usermodel.Row row : sheet) {
                for (int i = 0; i < row.getLastCellNum(); i++) {
                    csv.append(row.getCell(i)).append(i == row.getLastCellNum() - 1 ? "" : ",");
                }
                csv.append("\n");
            }
        }
        return csv.toString().getBytes();
    }
}