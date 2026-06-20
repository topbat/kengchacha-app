package com.kengchacha.voice;

import com.kengchacha.common.ApiResponse;
import com.kengchacha.voice.dto.AsrRequest;
import com.kengchacha.voice.dto.AsrResult;
import com.kengchacha.voice.dto.TtsRequest;
import com.kengchacha.voice.dto.TtsResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;

/**
 * 语音 ASR/TTS。多模态低门槛入口：语音输入/搜索/播报（适老化）。
 * 在线优先走前端 Web Speech；离线/兜底与生产云引擎统一经 VoiceClient。
 */
@RestController
@RequestMapping("/api/voice")
public class VoiceController {

    private final VoiceClient voiceClient;

    public VoiceController(VoiceClient voiceClient) {
        this.voiceClient = voiceClient;
    }

    /** 语音转文字。 */
    @PostMapping("/asr")
    public ApiResponse<AsrResult> asr(@RequestBody AsrRequest req) {
        byte[] audio = null;
        if (req.audioBase64() != null && !req.audioBase64().isBlank()) {
            try {
                audio = Base64.getDecoder().decode(stripDataUrl(req.audioBase64()));
            } catch (IllegalArgumentException ignore) {
                // 非法 base64 视为无音频，走 hint 兜底
            }
        }
        return ApiResponse.ok(voiceClient.asr(audio, req.format(), req.hint()));
    }

    /** 文本转语音（含 SSML 朗读脚本）。 */
    @PostMapping("/tts")
    public ApiResponse<TtsResult> tts(@Valid @RequestBody TtsRequest req) {
        return ApiResponse.ok(voiceClient.tts(req.text(), req.voice()));
    }

    private static String stripDataUrl(String s) {
        int comma = s.indexOf(',');
        return s.startsWith("data:") && comma >= 0 ? s.substring(comma + 1) : s;
    }
}
