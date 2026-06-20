package com.kengchacha.voice.dto;

/** 语音识别结果。engine 标识来源（browser-relay / mock / 讯飞 / 阿里云）。 */
public record AsrResult(String text, double confidence, String engine) {
}
