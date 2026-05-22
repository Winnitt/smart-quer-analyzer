package com.ai.analytics.smart_query.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
public class TableMetadata {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String tableName;
    @Column(length = 1000)
    private String columns; // Store as "id, product, amount..."
    private LocalDateTime uploadedAt;
}