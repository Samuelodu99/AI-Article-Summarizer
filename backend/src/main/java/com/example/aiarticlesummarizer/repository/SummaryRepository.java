package com.example.aiarticlesummarizer.repository;

import com.example.aiarticlesummarizer.model.Summary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SummaryRepository extends JpaRepository<Summary, Long> {

    Page<Summary> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<Summary> findTop10ByOrderByCreatedAtDesc();

    @Query("SELECT s FROM Summary s WHERE " +
           "LOWER(s.summary) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(s.originalContent) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(s.articleTitle) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "ORDER BY s.createdAt DESC")
    List<Summary> searchSummaries(@Param("query") String query);

    List<Summary> findBySourceUrl(String sourceUrl);

    void deleteByCreatedAtBefore(LocalDateTime cutoffDate);
}
