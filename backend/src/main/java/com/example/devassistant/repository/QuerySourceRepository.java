package com.example.devassistant.repository;

import com.example.devassistant.model.QuerySource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuerySourceRepository extends JpaRepository<QuerySource, Long> {
    List<QuerySource> findByQueryId(Long queryId);
}
