package com.kengchacha.recommend.dto;

import java.util.List;

/** 推荐/相似案例列表项：含相似度与推荐理由（可解释推荐）。 */
public record RecItem(Long id, String title, String slogan, String trick, Integer hazardLevel,
                      Integer hotScore, List<String> tags, int similarity, String reason) {
}
