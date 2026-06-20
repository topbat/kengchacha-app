package com.kengchacha.assistant;

/**
 * 大模型生成层抽象（对齐技术方案 §2.6 / §7：dev 与 prod 各提供实现，靠 Profile/配置切换）。
 * 开发期 MockLlmClient 离线确定性；生产可实现 QwenLlmClient/DeepSeekLlmClient 等调用云 API。
 */
public interface LlmClient {
    /** 根据提示词生成一句话自然语言小结（RAG 的“生成”步）。 */
    String complete(String prompt);
}
