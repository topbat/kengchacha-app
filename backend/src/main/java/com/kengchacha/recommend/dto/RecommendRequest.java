package com.kengchacha.recommend.dto;

import java.util.List;

/**
 * 个性化推荐入参。
 * interests：用户兴趣/短板标签（来自自测画像、所选筛选标签等）；
 * recentIds：近期浏览过的内容 id（用于构造兴趣向量并去重）；
 * size：返回条数。
 */
public record RecommendRequest(List<String> interests, List<Long> recentIds, Integer size) {
}
