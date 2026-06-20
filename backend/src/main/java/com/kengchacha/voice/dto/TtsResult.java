package com.kengchacha.voice.dto;

/**
 * 语音合成结果。
 * mime + audioBase64：可直接播放的音频（离线版为合成提示音）；
 * ssml：朗读脚本，前端可交给浏览器 speechSynthesis 实读（适老化播报）；
 * engine：来源标识。
 */
public record TtsResult(String mime, String audioBase64, String ssml, String engine) {
}
