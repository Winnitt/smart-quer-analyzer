package com.ai.analytics.smart_query.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
public class QueryHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String question;
    @Column(length = 2000)
    private String generatedSql;
    @Column(length = 2000)
    private String explanation;
    private String tableName;
    private LocalDateTime timestamp;
}