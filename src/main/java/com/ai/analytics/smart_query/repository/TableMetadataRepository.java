package com.ai.analytics.smart_query.repository;

import com.ai.analytics.smart_query.model.TableMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TableMetadataRepository extends JpaRepository<TableMetadata, Long> {
    Optional<TableMetadata> findByTableName(String tableName);
}