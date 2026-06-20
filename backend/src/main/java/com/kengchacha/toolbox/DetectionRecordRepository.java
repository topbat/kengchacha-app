package com.kengchacha.toolbox;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DetectionRecordRepository extends JpaRepository<DetectionRecord, Long> {
    List<DetectionRecord> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
