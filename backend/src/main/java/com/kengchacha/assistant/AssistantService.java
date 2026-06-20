package com.kengchacha.assistant;

import com.kengchacha.assistant.dto.ChatAnswer;
import com.kengchacha.assistant.dto.ChatAnswer.RefCard;
import com.kengchacha.content.Content;
import com.kengchacha.content.ContentRepository;
import com.kengchacha.content.ContentTag;
import com.kengchacha.content.ContentTagRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * AI 避坑助手：RAG-lite。
 * ① 检索：在案例库按“词表命中 + 标签命中”打分召回 Top 案例；
 * ② 生成：固定五段式结构化作答（判定→套路类型→套路推演→你该怎么做→关联案例），
 *    并经 LlmClient 产出一句话小结（开发期 Mock 离线，prod 切云模型）。
 */
@Service
public class AssistantService {

    private static final List<String> VOCAB = List.of(
            "转账", "贷款", "注销", "校园贷", "征信", "兼职", "刷单", "打字员", "押金", "保证金", "会员费",
            "保健品", "养生", "讲座", "鸡蛋", "荐股", "原始股", "导师", "股票", "投资", "虚拟币", "基金",
            "公检法", "警察", "检察官", "安全账户", "客服", "二房东", "租房", "房东", "换脸", "视频", "借钱",
            "充值", "游戏", "退款", "退费", "装修", "增项", "玉石", "直播", "鉴定", "培训", "高薪", "杀猪盘",
            "微信", "电话", "短信", "链接", "月子", "相亲", "婚恋", "中奖", "返利", "免费");

    private static final List<String> GENERIC_STEPS = List.of(
            "不要在催促下转账、付款或提供验证码；先停下来核实",
            "保留聊天记录、转账凭证等证据",
            "涉诈骗拨打 96110（反诈专线）/110；消费维权 12315；法律援助 12348");

    private static final String DISCLAIMER =
            "AI 辅助判断，不构成法律或官方结论，请以官方机构核实为准。";

    private final ContentRepository contentRepository;
    private final ContentTagRepository contentTagRepository;
    private final LlmClient llmClient;

    public AssistantService(ContentRepository contentRepository,
                            ContentTagRepository contentTagRepository,
                            LlmClient llmClient) {
        this.contentRepository = contentRepository;
        this.contentTagRepository = contentTagRepository;
        this.llmClient = llmClient;
    }

    public ChatAnswer chat(String message, String inputType) {
        String msg = message == null ? "" : message;
        List<Content> all = contentRepository.findAll();
        Map<Long, List<ContentTag>> tagMap = contentTagRepository.findAll().stream()
                .collect(Collectors.groupingBy(ContentTag::getContentId));

        List<Scored> scored = new ArrayList<>();
        for (Content c : all) {
            List<ContentTag> tags = tagMap.getOrDefault(c.getId(), List.of());
            String hay = String.join(" ", nz(c.getTitle()), nz(c.getTrick()), nz(c.getTip()),
                    nz(c.getBody()), nz(c.getSlogan()),
                    tags.stream().map(ContentTag::getTag).collect(Collectors.joining(" ")));
            int score = 0;
            for (String term : VOCAB) {
                if (msg.contains(term) && hay.contains(term)) score++;
            }
            for (ContentTag t : tags) {
                if (msg.contains(t.getTag())) score += 2;
            }
            if (score > 0) scored.add(new Scored(c, tags, score));
        }
        scored.sort((a, b) -> b.score() != a.score()
                ? Integer.compare(b.score(), a.score())
                : Integer.compare(hot(b.content()), hot(a.content())));

        String aiSummary = llmClient.complete(buildPrompt(msg, scored));

        if (scored.isEmpty()) {
            List<RefCard> recs = all.stream()
                    .sorted(Comparator.comparingInt((Content c) -> hot(c)).reversed())
                    .limit(3)
                    .map(this::toRef)
                    .toList();
            return new ChatAnswer(
                    "暂未在案例库匹配到高度相似的坑，但请保持警惕。",
                    "通用风险防范",
                    "诈骗常以“紧迫、权威、利诱、情感”四类手段操控你做出冲动决定。",
                    GENERIC_STEPS, recs, aiSummary, DISCLAIMER);
        }

        Scored best = scored.get(0);
        String domain = firstTag(best.tags(), 2);
        String psych = firstTag(best.tags(), 3);

        String judgement = switch (Optional.ofNullable(best.content().getHazardLevel()).orElse(2)) {
            case 3 -> "⚠ 高度疑似「" + nzTag(domain) + "」骗局，请高度警惕！";
            case 2 -> "疑似「" + nzTag(domain) + "」陷阱，建议多留个心眼。";
            default -> "可能涉及「" + nzTag(domain) + "」，注意防范。";
        };
        String trickType = Stream.of(domain, psych).filter(Objects::nonNull)
                .collect(Collectors.joining(" · "));

        List<String> steps = new ArrayList<>();
        if (best.content().getTip() != null) steps.add(best.content().getTip());
        steps.addAll(GENERIC_STEPS);

        List<RefCard> refs = scored.stream().limit(3).map(s -> toRef(s.content())).toList();

        return new ChatAnswer(judgement,
                trickType.isBlank() ? "风险提示" : trickType,
                nz(best.content().getTrick()),
                steps, refs, aiSummary, DISCLAIMER);
    }

    private String buildPrompt(String msg, List<Scored> scored) {
        String ctx = scored.stream().limit(3)
                .map(s -> "- " + s.content().getTitle() + "：" + nz(s.content().getTip()))
                .collect(Collectors.joining("\n"));
        return "你是防坑科普助手，只做防御性提示，不得输出任何实施诈骗或规避监管的内容。\n"
                + "用户问题：" + msg + "\n参考案例：\n" + ctx
                + "\n请用一句话给出友好提醒，并提示以官方渠道核实。";
    }

    private RefCard toRef(Content c) {
        return new RefCard(c.getId(), c.getTitle(), c.getSlogan(), c.getHazardLevel());
    }

    private static int hot(Content c) { return Optional.ofNullable(c.getHotScore()).orElse(0); }
    private static String firstTag(List<ContentTag> tags, int dim) {
        return tags.stream().filter(t -> t.getDimension() == dim)
                .map(ContentTag::getTag).findFirst().orElse(null);
    }
    private static String nzTag(String s) { return s == null ? "风险" : s; }
    private static String nz(String s) { return s == null ? "" : s; }

    private record Scored(Content content, List<ContentTag> tags, int score) {
    }
}
