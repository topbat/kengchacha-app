package com.kengchacha.guardian.dto;

import java.time.LocalDateTime;
import java.util.List;

public record RelationView(Long id, String ownerName, String memberName, String relation,
                           String phoneMask, List<String> topics, boolean voiceFirst,
                           long alertCount, LocalDateTime createdAt) {
}
