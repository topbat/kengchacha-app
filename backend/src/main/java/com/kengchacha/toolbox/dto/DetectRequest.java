package com.kengchacha.toolbox.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 风险检测统一入参。
 * input：主输入（合同正文 / 链接 / 截图 OCR 文本 / 收款账号）；
 * hint：辅助信息（如收款核验时的“对方自称名称/商家名”，可空）。
 */
public record DetectRequest(@NotBlank(message = "请输入待检测内容") String input, String hint) {
}
