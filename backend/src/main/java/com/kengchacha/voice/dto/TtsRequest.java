package com.kengchacha.voice.dto;

import jakarta.validation.constraints.NotBlank;

/** 语音合成入参。voice：发音人（female/male/elder，离线版仅影响提示音）。 */
public record TtsRequest(@NotBlank(message = "请提供待合成文本") String text, String voice) {
}
