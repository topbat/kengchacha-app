package com.kengchacha.assistant.dto;

import java.util.List;

/** AI 避坑助手的结构化作答（固定五段 + 关联案例 + 免责声明）。 */
public record ChatAnswer(
        String judgement,        // 是不是坑 / 危险程度
        String trickType,        // 套路类型（关联五维分类）
        String deduction,        // 套路推演
        List<String> steps,      // 你该怎么做
        List<RefCard> references,// 关联真实案例
        String aiSummary,        // LLM 生成的一句话小结
        String disclaimer        // 免责声明
) {
    public record RefCard(Long id, String title, String slogan, Integer hazardLevel) {
    }
}
