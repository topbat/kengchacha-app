package com.kengchacha.quiz.dto;

/** 出题时下发的选项（不含正确答案，防作弊）。 */
public record OptionView(Long id, String content) {
}
