package com.example.devassistant.repository;

import com.example.devassistant.model.Query;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QueryRepository extends JpaRepository<Query, Long> {
    List<Query> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Query> findTop20ByOrderByCreatedAtDesc();
}
