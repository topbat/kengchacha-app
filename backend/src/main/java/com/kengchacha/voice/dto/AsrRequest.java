package com.kengchacha.voice.dto;

/**
 * 语音识别入参。
 * audioBase64：音频（可空）；format：wav/pcm/webm 等；
 * hint：前端 Web Speech 已识别的文本（在线优先），便于离线后端直接转写/留痕。
 */
public record AsrRequest(String audioBase64, String format, String hint) {
}
