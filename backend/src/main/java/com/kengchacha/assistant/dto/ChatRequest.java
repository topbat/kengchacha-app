package com.kengchacha.assistant.dto;

import jakarta.validation.constraints.NotBlank;

/** 用户提问。inputType 可选：text/voice/image/link（语音、拍照由前端转文字后传入）。 */
public record ChatRequest(
        @NotBlank(message = "请输入你的问题") String message,
        String inputType
) {
}
