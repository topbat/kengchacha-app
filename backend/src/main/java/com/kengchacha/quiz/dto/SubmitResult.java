package com.kengchacha.quiz.dto;

import java.util.List;

public record SubmitResult(
        int score,
        int correctCount,
        int total,
        String profileTitle,
        List<String> profileTags,
        List<DimScore> dimScores,
        List<String> riskScenes,
        List<String> advice
) {
}
