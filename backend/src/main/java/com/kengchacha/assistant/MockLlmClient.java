package com.kengchacha.assistant;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 离线规则版 LLM（开发期默认）。不联网、确定性输出。
 * 生产将其替换为云模型实现（配置 kengchacha.ai.provider=qwen 等）。
 */
@Component
@ConditionalOnProperty(name = "kengchacha.ai.provider", havingValue = "mock", matchIfMissing = true)
public class MockLlmClient implements LlmClient {

    @Override
    public String complete(String prompt) {
        return "（AI 离线规则版）已基于「坑查查」案例库为你分析，最终请以官方机构核实为准。";
    }
}
