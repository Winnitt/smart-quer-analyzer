package com.ai.analytics.smart_query.controller;

import com.ai.analytics.smart_query.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/data")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class DataIngestionController {

    private final StorageService storageService;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            String tableName = storageService.uploadFile(file);
            return ResponseEntity.ok(tableName); // Return JUST the table name
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error");
        }
    }
}