package com.kengchacha.content.dto;

import java.time.LocalDateTime;
import java.util.List;

/** 内容详情。 */
public record ContentDetail(
        Long id,
        String title,
        String victimGroup,
        String trick,
        String loss,
        String tip,
        String slogan,
        String body,
        Integer hazardLevel,
        Integer sourceType,
        String sourceRef,
        Double credibility,
        Integer lossAmount,
        Integer hotScore,
        LocalDateTime onlineAt,
        List<String> tags
) {
}
