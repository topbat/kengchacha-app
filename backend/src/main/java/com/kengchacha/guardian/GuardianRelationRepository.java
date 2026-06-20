package com.kengchacha.guardian;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GuardianRelationRepository extends JpaRepository<GuardianRelation, Long> {
    List<GuardianRelation> findAllByOrderByCreatedAtDesc();
}
