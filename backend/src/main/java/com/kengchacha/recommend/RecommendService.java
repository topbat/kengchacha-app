package com.kengchacha.recommend;

import com.kengchacha.content.Content;
import com.kengchacha.recommend.dto.RecItem;
import com.kengchacha.recommend.dto.RecommendRequest;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 个性化推荐 / 相似案例（语义召回）。
 * 检索：兴趣标签 + 近期浏览 → 兴趣向量 → 向量近邻（VectorIndex）。
 * 可解释：用「命中的兴趣标签 / 同领域」生成推荐理由。
 */
@Service
public class RecommendService {

    private final VectorIndex index;

    public RecommendService(VectorIndex index) {
        this.index = index;
    }

    /** 相似案例：与某条内容语义最近的若干案例。 */
    public List<RecItem> similar(long contentId, int size) {
        int topK = clamp(size, 1, 12);
        return index.similar(contentId, topK).stream()
                .map(h -> toItem(h, Set.of(), "与当前案例套路相似"))
                .toList();
    }

    /** 为你推荐：兴趣标签 + 近期浏览 → 兴趣向量 → 召回。 */
    public List<RecItem> forYou(RecommendRequest req) {
        int size = clamp(req != null && req.size() != null ? req.size() : 6, 1, 12);
        List<String> interests = req != null && req.interests() != null ? req.interests() : List.of();
        List<Long> recent = req != null && req.recentIds() != null ? req.recentIds() : List.of();

        // 兴趣文本：标签直接拼，浏览过的内容补充标题/套路语义
        StringBuilder q = new StringBuilder(String.join(" ", interests));
        for (Long id : recent) {
            Content c = index.content(id);
            if (c != null) q.append(' ').append(nz(c.getTitle())).append(' ').append(nz(c.getTrick()));
        }

        Set<Long> exclude = new HashSet<>(recent);
        Set<String> interestSet = interests.stream().filter(Objects::nonNull)
                .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toSet());

        // 冷启动：无兴趣无浏览 → 回退热门高危
        if (q.toString().isBlank()) {
            return coldStart(size, exclude);
        }

        return index.search(q.toString(), exclude, size).stream()
                .map(h -> toItem(h, interestSet, null))
                .toList();
    }

    // ---------- helpers ----------

    private List<RecItem> coldStart(int size, Set<Long> exclude) {
        // 冷启动兜底：用「防坑·诈骗·高危」等通用词构造泛兴趣向量召回热门高危案例。
        return index.search("防坑 诈骗 高危 投资 兼职 转账 客服 保健品 租房", exclude, size).stream()
                .map(h -> toItem(h, Set.of(), "热门高危·先防为敬"))
                .toList();
    }

    private RecItem toItem(VectorIndex.Hit h, Set<String> interestSet, String fixedReason) {
        Content c = index.content(h.id());
        List<String> tags = index.tags(h.id());
        int sim = (int) Math.round(Math.min(1.0, h.score()) * 100);
        String reason = fixedReason != null ? fixedReason : buildReason(tags, interestSet);
        return new RecItem(c.getId(), c.getTitle(), c.getSlogan(), c.getTrick(),
                c.getHazardLevel(), c.getHotScore(), tags, sim, reason);
    }

    private static String buildReason(List<String> tags, Set<String> interestSet) {
        for (String t : tags) {
            if (interestSet.contains(t)) return "因为你关注「" + t + "」";
        }
        // 退而求其次：用领域类标签
        if (!tags.isEmpty()) return "你可能也想防「" + tags.get(0) + "」";
        return "猜你想防";
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
