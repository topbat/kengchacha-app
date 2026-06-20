package com.kengchacha.guardian;

import com.kengchacha.content.Content;
import com.kengchacha.content.ContentRepository;
import com.kengchacha.content.ContentTag;
import com.kengchacha.content.ContentTagRepository;
import com.kengchacha.guardian.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 家人守护：为家人订阅风险领域 → 扫描最新高危避坑内容 → 生成适老化「口播预警」推送。
 * 推送通道（短信/微信/IoT 音箱）由 guardian-service 在生产对接 MQ；本 MVP 落库为「站内提醒」。
 */
@Service
public class GuardianService {

    private final GuardianRelationRepository relationRepository;
    private final GuardianAlertRepository alertRepository;
    private final ContentRepository contentRepository;
    private final ContentTagRepository contentTagRepository;

    public GuardianService(GuardianRelationRepository relationRepository,
                           GuardianAlertRepository alertRepository,
                           ContentRepository contentRepository,
                           ContentTagRepository contentTagRepository) {
        this.relationRepository = relationRepository;
        this.alertRepository = alertRepository;
        this.contentRepository = contentRepository;
        this.contentTagRepository = contentTagRepository;
    }

    // ---------- 关系 ----------

    @Transactional
    public RelationView bind(BindRequest req) {
        GuardianRelation r = new GuardianRelation();
        r.setOwnerName(blankTo(req.ownerName(), "我"));
        r.setMemberName(req.memberName().trim());
        r.setRelation(blankTo(req.relation(), "家人"));
        r.setPhoneMask(maskPhone(req.phone()));
        r.setTopics(req.topics() == null ? "" : String.join(",", clean(req.topics())));
        r.setVoiceFirst(Boolean.TRUE.equals(req.voiceFirst()));
        r.setCreatedAt(LocalDateTime.now());
        return toRelationView(relationRepository.save(r), 0);
    }

    public List<RelationView> relations() {
        return relationRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(r -> toRelationView(r, alertRepository.findByRelationIdOrderByCreatedAtDesc(r.getId()).size()))
                .toList();
    }

    @Transactional
    public void unbind(Long relationId) {
        alertRepository.deleteAll(alertRepository.findByRelationIdOrderByCreatedAtDesc(relationId));
        relationRepository.deleteById(relationId);
    }

    // ---------- 预警推送 ----------

    /** 为单个家人扫描订阅领域的新高危内容并推送，返回新增预警。 */
    @Transactional
    public List<AlertView> push(Long relationId) {
        GuardianRelation r = relationRepository.findById(relationId)
                .orElseThrow(() -> new IllegalArgumentException("守护关系不存在"));
        return pushFor(r);
    }

    /** 一键守护：为所有家人扫描并推送。 */
    @Transactional
    public List<AlertView> pushAll() {
        List<AlertView> all = new ArrayList<>();
        for (GuardianRelation r : relationRepository.findAllByOrderByCreatedAtDesc()) {
            all.addAll(pushFor(r));
        }
        all.sort(Comparator.comparing(AlertView::createdAt).reversed());
        return all;
    }

    private List<AlertView> pushFor(GuardianRelation r) {
        Set<String> topics = new LinkedHashSet<>(splitTopics(r.getTopics()));
        Map<Long, List<ContentTag>> tagMap = contentTagRepository.findAll().stream()
                .collect(Collectors.groupingBy(ContentTag::getContentId));

        // 候选：高危内容（hazard=3），按热度排序，命中订阅领域（无订阅则全部高危）
        List<Content> candidates = contentRepository.findAll().stream()
                .filter(c -> Optional.ofNullable(c.getHazardLevel()).orElse(0) == 3)
                .sorted(Comparator.comparingInt((Content c) -> Optional.ofNullable(c.getHotScore()).orElse(0)).reversed())
                .toList();

        List<AlertView> created = new ArrayList<>();
        for (Content c : candidates) {
            String domain = domainOf(tagMap.getOrDefault(c.getId(), List.of()));
            if (!topics.isEmpty() && (domain == null || !topics.contains(domain))) continue;
            if (alertRepository.existsByRelationIdAndContentId(r.getId(), c.getId())) continue;

            GuardianAlert a = new GuardianAlert();
            a.setRelationId(r.getId());
            a.setContentId(c.getId());
            a.setMemberName(r.getMemberName());
            a.setTopic(domain == null ? "综合风险" : domain);
            a.setLevel(3);
            a.setTitle("【守护提醒】高危预警 · " + (domain == null ? "防诈" : domain));
            a.setBody(speechScript(r, c, domain));
            a.setReadFlag(false);
            a.setCreatedAt(LocalDateTime.now());
            created.add(toAlertView(alertRepository.save(a)));

            if (created.size() >= 3) break;   // 单次最多推 3 条，避免刷屏
        }
        return created;
    }

    /** 适老化口播稿：短句、点明套路与一句话避坑。 */
    private static String speechScript(GuardianRelation r, Content c, String domain) {
        String who = r.getMemberName();
        String slogan = c.getSlogan() != null ? c.getSlogan() : c.getTitle();
        String tip = firstSentence(c.getTip());
        return who + "您好，坑查查守护提醒：近期高发「" + (domain == null ? "诈骗" : domain) + "」套路——"
                + slogan + "。" + (tip.isEmpty() ? "" : tip + "。") + "凡涉及转账、验证码，先挂断、拨 96110 核实。";
    }

    // ---------- 预警列表 ----------

    public List<AlertView> alerts() {
        return alertRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toAlertView).toList();
    }

    @Transactional
    public void markRead(Long alertId) {
        alertRepository.findById(alertId).ifPresent(a -> {
            a.setReadFlag(true);
            alertRepository.save(a);
        });
    }

    public GuardianOverview overview() {
        List<RelationView> rels = relations();
        List<AlertView> alerts = alerts();
        return new GuardianOverview(rels.size(), alertRepository.countByReadFlagFalse(), rels, alerts);
    }

    // ---------- helpers ----------

    private RelationView toRelationView(GuardianRelation r, long alertCount) {
        return new RelationView(r.getId(), r.getOwnerName(), r.getMemberName(), r.getRelation(),
                r.getPhoneMask(), splitTopics(r.getTopics()), Boolean.TRUE.equals(r.getVoiceFirst()),
                alertCount, r.getCreatedAt());
    }

    private AlertView toAlertView(GuardianAlert a) {
        return new AlertView(a.getId(), a.getRelationId(), a.getContentId(), a.getMemberName(),
                a.getTopic(), Optional.ofNullable(a.getLevel()).orElse(3), a.getTitle(),
                a.getBody(), Boolean.TRUE.equals(a.getReadFlag()), a.getCreatedAt());
    }

    private static String domainOf(List<ContentTag> tags) {
        return tags.stream().filter(t -> t.getDimension() == 2)
                .map(ContentTag::getTag).findFirst().orElse(null);
    }

    private static List<String> splitTopics(String s) {
        if (s == null || s.isBlank()) return List.of();
        return Arrays.stream(s.split(",")).map(String::trim).filter(x -> !x.isEmpty()).toList();
    }

    private static List<String> clean(List<String> in) {
        return in.stream().filter(Objects::nonNull).map(String::trim).filter(x -> !x.isEmpty()).distinct().toList();
    }

    private static String maskPhone(String phone) {
        if (phone == null) return "";
        String d = phone.replaceAll("\\D", "");
        if (d.length() == 11) return d.substring(0, 3) + "****" + d.substring(7);
        return d.isEmpty() ? "" : "***";
    }

    private static String firstSentence(String s) {
        if (s == null) return "";
        String[] parts = s.split("[。；;]");
        return parts.length > 0 ? parts[0].trim() : s.trim();
    }

    private static String blankTo(String s, String def) {
        return s == null || s.isBlank() ? def : s.trim();
    }
}
