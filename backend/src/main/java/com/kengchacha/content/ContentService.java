package com.kengchacha.content;

import com.kengchacha.common.PageResult;
import com.kengchacha.content.dto.ContentCard;
import com.kengchacha.content.dto.ContentDetail;
import com.kengchacha.content.dto.TagGroup;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ContentService {

    private static final Map<Integer, String> DIM_NAMES = Map.of(
            1, "人生阶段", 2, "领域", 3, "心理机制", 4, "渠道", 5, "损失类型");

    private final ContentRepository contentRepository;
    private final ContentTagRepository contentTagRepository;

    public ContentService(ContentRepository contentRepository, ContentTagRepository contentTagRepository) {
        this.contentRepository = contentRepository;
        this.contentTagRepository = contentTagRepository;
    }

    /** 五维筛选 + 关键词 + 分页。内容量小，内存过滤足矣（演进可下推到 ES）。 */
    public PageResult<ContentCard> feed(String stage, String domain, String psych,
                                        Integer hazard, String q, int page, int size) {
        List<Content> all = contentRepository.findAll(Sort.by(Sort.Direction.DESC, "hotScore"));
        Map<Long, List<ContentTag>> tagMap = tagMapOf(all);

        List<ContentCard> filtered = all.stream()
                .filter(c -> matches(c, tagMap.getOrDefault(c.getId(), List.of()),
                        stage, domain, psych, hazard, q))
                .map(c -> toCard(c, tagMap.getOrDefault(c.getId(), List.of())))
                .toList();

        int from = Math.min(page * size, filtered.size());
        int to = Math.min(from + size, filtered.size());
        return new PageResult<>(filtered.subList(from, to), filtered.size(), page, size);
    }

    public ContentDetail detail(Long id) {
        Content c = contentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("内容不存在"));
        List<String> tags = tagMapOf(List.of(c)).getOrDefault(id, List.of()).stream()
                .map(ContentTag::getTag).distinct().toList();
        return new ContentDetail(c.getId(), c.getTitle(), c.getVictimGroup(), c.getTrick(), c.getLoss(),
                c.getTip(), c.getSlogan(), c.getBody(), c.getHazardLevel(), c.getSourceType(), c.getSourceRef(),
                c.getCredibility(), c.getLossAmount(), c.getHotScore(), c.getOnlineAt(), tags);
    }

    /** 五维标签字典，供前端筛选条。 */
    public List<TagGroup> tagGroups() {
        Map<Integer, LinkedHashSet<String>> byDim = new TreeMap<>();
        for (ContentTag t : contentTagRepository.findAll()) {
            byDim.computeIfAbsent(t.getDimension(), k -> new LinkedHashSet<>()).add(t.getTag());
        }
        return byDim.entrySet().stream()
                .map(e -> new TagGroup(e.getKey(),
                        DIM_NAMES.getOrDefault(e.getKey(), "其他"),
                        new ArrayList<>(e.getValue())))
                .toList();
    }

    // ---------- helpers ----------

    private Map<Long, List<ContentTag>> tagMapOf(List<Content> contents) {
        if (contents.isEmpty()) return Map.of();
        List<Long> ids = contents.stream().map(Content::getId).toList();
        return contentTagRepository.findByContentIdIn(ids).stream()
                .collect(Collectors.groupingBy(ContentTag::getContentId));
    }

    private boolean matches(Content c, List<ContentTag> tags, String stage, String domain,
                            String psych, Integer hazard, String q) {
        if (hazard != null && !hazard.equals(c.getHazardLevel())) return false;
        if (hasText(stage) && !hasTag(tags, 1, stage)) return false;
        if (hasText(domain) && !hasTag(tags, 2, domain)) return false;
        if (hasText(psych) && !hasTag(tags, 3, psych)) return false;
        if (hasText(q)) {
            String hay = String.join(" ", nz(c.getTitle()), nz(c.getTrick()),
                    nz(c.getTip()), nz(c.getBody()), nz(c.getVictimGroup()));
            if (!hay.contains(q.trim())) return false;
        }
        return true;
    }

    private boolean hasTag(List<ContentTag> tags, int dim, String value) {
        return tags.stream().anyMatch(t -> t.getDimension() == dim && t.getTag().equals(value));
    }

    private ContentCard toCard(Content c, List<ContentTag> tags) {
        List<String> flat = tags.stream()
                .filter(t -> t.getDimension() <= 4)   // 阶段/领域/心理/渠道用于展示
                .map(ContentTag::getTag).distinct().toList();
        return new ContentCard(c.getId(), c.getTitle(), c.getVictimGroup(), c.getTrick(), c.getLoss(),
                c.getTip(), c.getSlogan(), c.getHazardLevel(), c.getSourceType(), c.getSourceRef(),
                c.getHotScore(), flat);
    }

    private static boolean hasText(String s) { return s != null && !s.isBlank(); }
    private static String nz(String s) { return s == null ? "" : s; }
}
