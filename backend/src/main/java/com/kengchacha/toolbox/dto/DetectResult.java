package com.kengchacha.toolbox.dto;

import java.util.List;

/**
 * 风险检测结果。
 * toolType 1合同体检 2链接验毒 3拍照识坑 4收款核验
 * riskLevel 1低 2中 3高；score 0-100 风险分（越高越危险）。
 */
public record DetectResult(int toolType, int riskLevel, int score, String verdict,
                           String summary, List<RiskHit> hits, List<String> advice, String disclaimer) {
}
