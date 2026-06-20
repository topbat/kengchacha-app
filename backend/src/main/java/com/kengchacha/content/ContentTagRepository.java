package com.kengchacha.content;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContentTagRepository extends JpaRepository<ContentTag, Long> {
    List<ContentTag> findByContentIdIn(List<Long> contentIds);
}
