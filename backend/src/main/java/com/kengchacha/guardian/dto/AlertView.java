package com.kengchacha.guardian.dto;

import java.time.LocalDateTime;

public record AlertView(Long id, Long relationId, Long contentId, String memberName, String topic,
                        int level, String title, String body, boolean readFlag, LocalDateTime createdAt) {
}
