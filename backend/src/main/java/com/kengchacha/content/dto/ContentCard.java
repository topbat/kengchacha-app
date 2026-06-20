package com.kengchacha.content.dto;

import java.util.List;

/** 信息流卡片。 */
public record ContentCard(
        Long id,
        String title,
        String victimGroup,
        String trick,
        String loss,
        String tip,
        String slogan,
        Integer hazardLevel,   // 1低 2中 3高
        Integer sourceType,    // 1真实经历 2官方预警 3判例投诉
        String sourceRef,
        Integer hotScore,
        List<String> tags      // 扁平标签，用于卡片展示
) {
}
