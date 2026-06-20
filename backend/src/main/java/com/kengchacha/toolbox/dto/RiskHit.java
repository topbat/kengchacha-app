package com.kengchacha.toolbox.dto;

/**
 * 单条风险命中。
 * label   命中的风险/套路名称
 * snippet 命中的原文片段（标红依据）
 * detail  为什么有风险
 * advice  针对该条的处置建议
 * severity 1低 2中 3高
 */
public record RiskHit(String label, String snippet, String detail, String advice, int severity) {
}
