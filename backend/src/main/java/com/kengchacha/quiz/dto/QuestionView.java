package com.kengchacha.quiz.dto;

import java.util.List;

public record QuestionView(Long id, String stem, String dimension, List<OptionView> options) {
}
