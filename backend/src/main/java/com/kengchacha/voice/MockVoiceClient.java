package com.kengchacha.voice;

import com.kengchacha.voice.dto.AsrResult;
import com.kengchacha.voice.dto.TtsResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 离线规则版语音（开发期默认）：
 *  - ASR：在线优先（前端 Web Speech 已识别则回传 hint 留痕）；无 hint 时给出引导文案；
 *  - TTS：返回可直接播放的「确定性提示音 WAV」（纯数学合成，无依赖）+ SSML 朗读脚本，
 *         真正的逐字朗读由前端 speechSynthesis 实读（适老化播报）。
 * 生产替换为讯飞/阿里云实现（kengchacha.ai.voice=xfyun 等），接口不变。
 */
@Component
@ConditionalOnProperty(name = "kengchacha.ai.voice", havingValue = "mock", matchIfMissing = true)
public class MockVoiceClient implements VoiceClient {

    @Override
    public AsrResult asr(byte[] audio, String format, String hint) {
        if (hint != null && !hint.isBlank()) {
            return new AsrResult(hint.trim(), 0.99, "browser-relay");
        }
        if (audio != null && audio.length > 0) {
            // 离线无法真实解码，给出占位转写并提示在线引擎
            return new AsrResult("（离线版未配置云 ASR，已收到 " + audio.length
                    + " 字节音频）建议使用浏览器语音输入，或在生产接入讯飞/阿里云。", 0.3, "mock");
        }
        return new AsrResult("", 0.0, "mock");
    }

    @Override
    public TtsResult tts(String text, String voice) {
        byte[] wav = synthBeep(text, voice);
        String b64 = Base64.getEncoder().encodeToString(wav);
        String ssml = "<speak>" + escape(text) + "</speak>";
        return new TtsResult("audio/wav", b64, ssml, "mock");
    }

    // ---------- 确定性提示音合成（16kHz / 16bit / 单声道 WAV） ----------

    private static byte[] synthBeep(String text, String voice) {
        final int sr = 16000;
        double dur = Math.min(1.2, 0.30 + (text == null ? 0 : text.length()) * 0.02);
        int n = (int) (sr * dur);

        int seed = (text == null ? 0 : text.hashCode());
        double base = 380 + Math.floorMod(seed, 8) * 36;          // 基频随文本变化
        if ("elder".equalsIgnoreCase(voice)) base *= 0.85;         // 适老：略低更清晰
        if ("male".equalsIgnoreCase(voice)) base *= 0.8;
        double f2 = base * 1.5;
        int fade = (int) (0.02 * sr);

        short[] pcm = new short[n];
        for (int i = 0; i < n; i++) {
            double t = (double) i / sr;
            double env = Math.min(1.0, (double) Math.min(i, n - i) / fade);
            double v = Math.sin(2 * Math.PI * base * t) * 0.5 + Math.sin(2 * Math.PI * f2 * t) * 0.22;
            pcm[i] = (short) (v * env * 0.6 * Short.MAX_VALUE);
        }
        return wav(pcm, sr);
    }

    private static byte[] wav(short[] pcm, int sampleRate) {
        int dataLen = pcm.length * 2;
        ByteArrayOutputStream out = new ByteArrayOutputStream(44 + dataLen);
        writeStr(out, "RIFF");
        writeIntLE(out, 36 + dataLen);
        writeStr(out, "WAVE");
        writeStr(out, "fmt ");
        writeIntLE(out, 16);            // PCM 子块大小
        writeShortLE(out, 1);           // PCM
        writeShortLE(out, 1);           // 单声道
        writeIntLE(out, sampleRate);
        writeIntLE(out, sampleRate * 2);// 字节率
        writeShortLE(out, 2);           // 块对齐
        writeShortLE(out, 16);          // 位深
        writeStr(out, "data");
        writeIntLE(out, dataLen);
        for (short s : pcm) writeShortLE(out, s);
        return out.toByteArray();
    }

    private static void writeStr(ByteArrayOutputStream o, String s) {
        o.writeBytes(s.getBytes(StandardCharsets.US_ASCII));
    }

    private static void writeIntLE(ByteArrayOutputStream o, int v) {
        o.write(v & 0xFF);
        o.write((v >> 8) & 0xFF);
        o.write((v >> 16) & 0xFF);
        o.write((v >> 24) & 0xFF);
    }

    private static void writeShortLE(ByteArrayOutputStream o, int v) {
        o.write(v & 0xFF);
        o.write((v >> 8) & 0xFF);
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
