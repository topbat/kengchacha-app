package com.kengchacha.quiz.dto;

import java.util.List;

public record StartResult(int scale, int mode, List<QuestionView> questions) {
}
