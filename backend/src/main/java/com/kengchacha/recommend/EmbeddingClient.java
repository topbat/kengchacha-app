package com.kengchacha.recommend;

/**
 * 向量化（Embedding）层抽象（对齐技术方案 §2.6 / §6：BGE/M3E 或向量化云 API）。
 * 开发期 MockEmbeddingClient 离线确定性；生产可实现 BgeEmbeddingClient 调用本地/云模型，
 * 接口与上层（向量索引、RAG、相似案例、个性化推荐）保持不变。
 */
public interface EmbeddingClient {

    /** 文本 → 稠密向量（已 L2 归一化，便于点积即余弦）。 */
    float[] embed(String text);

    /** 向量维度。 */
    int dimension();
}
