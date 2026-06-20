package com.kengchacha.recommend;

import com.kengchacha.content.Content;
import com.kengchacha.content.ContentRepository;
import com.kengchacha.content.ContentTag;
import com.kengchacha.content.ContentTagRepository;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 内容向量索引（pgvector / Milvus 的开发期内存替身）。
 * 懒加载：首次使用时为全部案例计算 Embedding 并缓存；提供「按向量近邻检索」。
 * 生产切 pgvector/Milvus 时，仅替换 search/similar 的存储与召回实现，上层不变。
 */
@Component
public class VectorIndex {

    private final ContentRepository contentRepository;
    private final ContentTagRepository contentTagRepository;
    private final EmbeddingClient embeddingClient;

    private volatile boolean ready = false;
    private long[] ids = new long[0];
    private float[][] vecs = new float[0][];
    private Map<Long, Content> byId = Map.of();
    private Map<Long, List<String>> tagsById = Map.of();

    public VectorIndex(ContentRepository contentRepository,
                       ContentTagRepository contentTagRepository,
                       EmbeddingClient embeddingClient) {
        this.contentRepository = contentRepository;
        this.contentTagRepository = contentTagRepository;
        this.embeddingClient = embeddingClient;
    }

    private void ensureBuilt() {
        if (ready) return;
        synchronized (this) {
            if (ready) return;
            List<Content> all = contentRepository.findAll();
            Map<Long, List<ContentTag>> tagMap = contentTagRepository.findAll().stream()
                    .collect(Collectors.groupingBy(ContentTag::getContentId));

            ids = new long[all.size()];
            vecs = new float[all.size()][];
            Map<Long, Content> m = new HashMap<>();
            Map<Long, List<String>> tm = new HashMap<>();
            for (int i = 0; i < all.size(); i++) {
                Content c = all.get(i);
                List<String> tags = tagMap.getOrDefault(c.getId(), List.of()).stream()
                        .map(ContentTag::getTag).distinct().toList();
                ids[i] = c.getId();
                vecs[i] = embeddingClient.embed(docText(c, tags));
                m.put(c.getId(), c);
                tm.put(c.getId(), tags);
            }
            byId = m;
            tagsById = tm;
            ready = true;
        }
    }

    private static String docText(Content c, List<String> tags) {
        return String.join(" ",
                nz(c.getTitle()), nz(c.getTrick()), nz(c.getTip()),
                nz(c.getSlogan()), nz(c.getBody()), nz(c.getVictimGroup()),
                String.join(" ", tags));
    }

    /** 与某案例最相似的 topK（按余弦，排除自身）。 */
    public List<Hit> similar(long contentId, int topK) {
        ensureBuilt();
        int self = indexOf(contentId);
        if (self < 0) return List.of();
        return rank(vecs[self], Set.of(contentId), topK);
    }

    /** 按查询文本做近邻检索（个性化推荐的“兴趣向量”入口）。 */
    public List<Hit> search(String queryText, Set<Long> exclude, int topK) {
        ensureBuilt();
        if (queryText == null || queryText.isBlank()) return List.of();
        return rank(embeddingClient.embed(queryText), exclude, topK);
    }

    public Content content(long id) {
        ensureBuilt();
        return byId.get(id);
    }

    public List<String> tags(long id) {
        ensureBuilt();
        return tagsById.getOrDefault(id, List.of());
    }

    // ---------- 内部 ----------

    private List<Hit> rank(float[] query, Set<Long> exclude, int topK) {
        List<Hit> hits = new ArrayList<>();
        for (int i = 0; i < ids.length; i++) {
            if (exclude.contains(ids[i])) continue;
            double sim = dot(query, vecs[i]);
            if (sim <= 0) continue;
            hits.add(new Hit(ids[i], sim));
        }
        hits.sort((a, b) -> Double.compare(b.score(), a.score()));
        return hits.size() > topK ? hits.subList(0, topK) : hits;
    }

    private int indexOf(long id) {
        for (int i = 0; i < ids.length; i++) if (ids[i] == id) return i;
        return -1;
    }

    private static double dot(float[] a, float[] b) {
        double s = 0;
        int n = Math.min(a.length, b.length);
        for (int i = 0; i < n; i++) s += a[i] * b[i];
        return s;
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    /** 检索命中：内容 id + 相似度（0~1）。 */
    public record Hit(long id, double score) {
    }
}
