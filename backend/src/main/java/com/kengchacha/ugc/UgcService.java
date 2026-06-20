package com.kengchacha.ugc;

import com.kengchacha.common.PageResult;
import com.kengchacha.ugc.dto.CreateStoryRequest;
import com.kengchacha.ugc.dto.StoryView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UgcService {

    /** 轻量内容安全占位：命中即驳回（生产替换为云内容安全 + 人工复审）。 */
    private static final List<String> BANNED = List.of("加微信领", "代开发票", "包过保过", "免费送钱");

    private final UgcStoryRepository repo;

    public UgcService(UgcStoryRepository repo) {
        this.repo = repo;
    }

    public PageResult<StoryView> list(int page, int size) {
        Page<UgcStory> p = repo.findByAuditStatusInOrderByCreatedAtDesc(
                List.of(1, 2), PageRequest.of(page, size));
        List<StoryView> items = p.getContent().stream().map(this::toView).toList();
        return new PageResult<>(items, p.getTotalElements(), page, size);
    }

    public StoryView create(CreateStoryRequest req) {
        // AI 初审（占位）：敏感词命中直接驳回
        boolean rejected = BANNED.stream().anyMatch(req.content()::contains);

        UgcStory s = new UgcStory();
        s.setNickname(blankTo(req.nickname(), "热心网友"));
        s.setHappenTime(req.happenTime());
        s.setRegion(req.region());
        s.setGroupTag(req.groupTag());
        s.setDomainTag(req.domainTag());
        s.setContent(req.content());
        s.setAdvice(req.advice());
        s.setLikeLearn(0);
        s.setLikePity(0);
        s.setAuditStatus(rejected ? 3 : 1);   // 1=AI通过（待人工复审）
        s.setCreatedAt(LocalDateTime.now());
        repo.save(s);

        if (rejected) {
            throw new IllegalArgumentException("内容包含疑似违规信息，已被拦截，请修改后重试");
        }
        return toView(s);
    }

    /** 点赞：type=learn(学到了) / pity(点亮·同情)。 */
    public StoryView like(Long id, String type) {
        UgcStory s = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("记录不存在"));
        if ("pity".equals(type)) {
            s.setLikePity(orZero(s.getLikePity()) + 1);
        } else {
            s.setLikeLearn(orZero(s.getLikeLearn()) + 1);
        }
        repo.save(s);
        return toView(s);
    }

    private StoryView toView(UgcStory s) {
        return new StoryView(s.getId(), s.getNickname(), s.getHappenTime(), s.getRegion(),
                s.getGroupTag(), s.getDomainTag(), s.getContent(), s.getAdvice(),
                orZero(s.getLikeLearn()), orZero(s.getLikePity()), s.getCreatedAt());
    }

    private static int orZero(Integer i) { return i == null ? 0 : i; }
    private static String blankTo(String s, String def) { return (s == null || s.isBlank()) ? def : s; }
}
