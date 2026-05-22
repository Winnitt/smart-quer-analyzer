package com.ai.analytics.smart_query.controller;

import com.ai.analytics.smart_query.model.TableMetadata;
import com.ai.analytics.smart_query.repository.TableMetadataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/metadata")
@RequiredArgsConstructor
@CrossOrigin("*")

public class MetadataController {
    private final TableMetadataRepository repository;

    @GetMapping("/tables")
    public List<TableMetadata> getTables() {
        return repository.findAll();
    }
}