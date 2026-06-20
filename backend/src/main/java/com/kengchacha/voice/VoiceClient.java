package com.kengchacha.voice;

import com.kengchacha.voice.dto.AsrResult;
import com.kengchacha.voice.dto.TtsResult;

/**
 * 语音能力抽象（对齐技术方案 §6：讯飞 / 阿里云 / Whisper 自部署）。
 * 开发期 MockVoiceClient 离线确定性；生产实现对接云 ASR/TTS，上层接口不变。
 */
public interface VoiceClient {

    /** 语音转写：audio 可空（在线优先时由前端 Web Speech 提供 hint 文本）。 */
    AsrResult asr(byte[] audio, String format, String hint);

    /** 文本合成：返回可播放音频 + 朗读脚本（适老化播报）。 */
    TtsResult tts(String text, String voice);
}
