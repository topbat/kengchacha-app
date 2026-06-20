package com.kengchacha.content.dto;

import java.util.List;

/** 五维标签字典（供前端筛选条）。 */
public record TagGroup(int dimension, String dimensionName, List<String> tags) {
}
