package com.ai.analytics.smart_query.repository;

import com.ai.analytics.smart_query.model.QueryHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface QueryHistoryRepository extends JpaRepository<QueryHistory, Long> {
    List<QueryHistory> findTop10ByOrderByTimestampDesc(); // Get last 10 queries
}