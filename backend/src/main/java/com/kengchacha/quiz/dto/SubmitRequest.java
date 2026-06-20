package com.kengchacha.quiz.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SubmitRequest(
        Integer scale,
        Integer mode,
        @NotEmpty(message = "答案不能为空") List<Answer> answers
) {
    public record Answer(Long questionId, Long optionId) {
    }
}
