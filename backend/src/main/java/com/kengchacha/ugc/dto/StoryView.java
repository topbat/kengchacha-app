package com.kengchacha.ugc.dto;

import java.time.LocalDateTime;

public record StoryView(
        Long id,
        String nickname,
        String happenTime,
        String region,
        String groupTag,
        String domainTag,
        String content,
        String advice,
        Integer likeLearn,
        Integer likePity,
        LocalDateTime createdAt
) {
}
