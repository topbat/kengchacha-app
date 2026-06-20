package com.kengchacha.guardian;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GuardianAlertRepository extends JpaRepository<GuardianAlert, Long> {
    List<GuardianAlert> findAllByOrderByCreatedAtDesc();

    List<GuardianAlert> findByRelationIdOrderByCreatedAtDesc(Long relationId);

    long countByReadFlagFalse();

    boolean existsByRelationIdAndContentId(Long relationId, Long contentId);
}
