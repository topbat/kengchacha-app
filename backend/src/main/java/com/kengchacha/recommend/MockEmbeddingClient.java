package com.kengchacha.recommend;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 离线规则版 Embedding（开发期默认）：中文按「字 + 相邻二元（bigram）」做特征哈希 + TF 加权，
 * 再 L2 归一化。确定性、不联网、无需显存；语义近似靠字面共现，足以驱动「相似案例/个性化推荐」演示。
 * 生产将其替换为 BGE/M3E 等真实模型实现（配置 kengchacha.ai.embedding=bge 等）。
 */
@Component
@ConditionalOnProperty(name = "kengchacha.ai.embedding", havingValue = "mock", matchIfMissing = true)
public class MockEmbeddingClient implements EmbeddingClient {

    private static final int DIM = 256;

    @Override
    public int dimension() {
        return DIM;
    }

    @Override
    public float[] embed(String text) {
        float[] v = new float[DIM];
        if (text == null || text.isBlank()) return v;
        String s = normalize(text);
        // 单字特征
        for (int i = 0; i < s.length(); i++) {
            add(v, feature(String.valueOf(s.charAt(i))), 1.0f);
        }
        // 相邻二元特征（更稳的局部语序信号），权重略高
        for (int i = 0; i + 1 < s.length(); i++) {
            add(v, feature(s.substring(i, i + 2)), 1.6f);
        }
        l2normalize(v);
        return v;
    }

    /** 仅保留中日文/字母/数字，过滤标点与空白，降低噪声。 */
    private static String normalize(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        for (char c : text.toLowerCase().toCharArray()) {
            if (Character.isLetterOrDigit(c)) sb.append(c);
        }
        return sb.toString();
    }

    private static int feature(String token) {
        int h = token.hashCode();
        h ^= (h >>> 16);
        return Math.floorMod(h, DIM);
    }

    private static void add(float[] v, int idx, float w) {
        v[idx] += w;
    }

    private static void l2normalize(float[] v) {
        double sum = 0;
        for (float x : v) sum += x * x;
        if (sum <= 0) return;
        float inv = (float) (1.0 / Math.sqrt(sum));
        for (int i = 0; i < v.length; i++) v[i] *= inv;
    }
}
