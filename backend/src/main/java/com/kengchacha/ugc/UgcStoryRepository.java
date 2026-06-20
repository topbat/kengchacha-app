package com.kengchacha.ugc;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

public interface UgcStoryRepository extends JpaRepository<UgcStory, Long> {
    Page<UgcStory> findByAuditStatusInOrderByCreatedAtDesc(Collection<Integer> statuses, Pageable pageable);
}
