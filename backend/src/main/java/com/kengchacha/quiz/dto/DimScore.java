package com.kengchacha.quiz.dto;

/** 单维度得分（用于雷达图）。 */
public record DimScore(String dimension, int correct, int total, int rate) {
}
