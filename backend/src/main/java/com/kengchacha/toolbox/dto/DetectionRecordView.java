package com.kengchacha.toolbox.dto;

import java.time.LocalDateTime;

/** 检测记录（私密）列表项：仅回传脱敏预览，不回传原文。 */
public record DetectionRecordView(Long id, int toolType, String toolName, String preview,
                                  int riskLevel, int score, LocalDateTime createdAt) {
}
