package com.kengchacha.quiz;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kengchacha.quiz.dto.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class QuizService {

    /** 维度 -> {画像标签, 高危场景, 可执行建议} */
    private static final Map<String, String[]> DIM_META = Map.of(
            "心理", new String[]{"沉没成本猎物", "容易因侥幸、沉没成本而越陷越深",
                    "做决定前先问自己：最坏会损失多少？该止损就止损"},
            "法律", new String[]{"法网外游侠", "在合同、借贷、担保条款上容易吃暗亏",
                    "签约/借贷前看清条款，分清借条与欠条、定金与订金"},
            "消费", new String[]{"贪促销体质", "易被预付卡、直播带货、限时促销冲动消费",
                    "预付别超额；冲动下单前强制冷静，认准“7 天冷静期”"},
            "金融", new String[]{"高收益易感人", "易被荐股、虚拟币、原始股、套路贷收割",
                    "凡承诺稳赚、引导到站外 App 投资的，一律拒绝"},
            "职场", new String[]{"职场小白", "易遇培训贷、试用期白嫖、入职押金",
                    "入职先向你收钱的一律拒绝；合同填写完整再签字"},
            "网络", new String[]{"易信人小白", "易中冒充客服/公检法、刷单、AI 换脸诈骗",
                    "凡“安全账户/先转账”先挂断，拨打 96110 核实"}
    );

    private final QuizQuestionRepository questionRepo;
    private final QuizOptionRepository optionRepo;
    private final TestRecordRepository recordRepo;
    private final ObjectMapper objectMapper;

    public QuizService(QuizQuestionRepository questionRepo, QuizOptionRepository optionRepo,
                       TestRecordRepository recordRepo, ObjectMapper objectMapper) {
        this.questionRepo = questionRepo;
        this.optionRepo = optionRepo;
        this.recordRepo = recordRepo;
        this.objectMapper = objectMapper;
    }

    /** 出题：随机取 scale 道（种子题量有限时取全部），不下发正确答案。 */
    public StartResult start(int scale, int mode) {
        List<QuizQuestion> all = new ArrayList<>(questionRepo.findAll());
        Collections.shuffle(all);
        List<QuizQuestion> picked = all.subList(0, Math.min(scale, all.size()));

        List<Long> qids = picked.stream().map(QuizQuestion::getId).toList();
        Map<Long, List<QuizOption>> optMap = optionRepo.findByQuestionIdIn(qids).stream()
                .collect(Collectors.groupingBy(QuizOption::getQuestionId));

        List<QuestionView> views = picked.stream().map(q -> {
            List<OptionView> opts = optMap.getOrDefault(q.getId(), List.of()).stream()
                    .sorted(Comparator.comparing(o -> Optional.ofNullable(o.getSort()).orElse(0)))
                    .map(o -> new OptionView(o.getId(), o.getContent()))
                    .toList();
            return new QuestionView(q.getId(), q.getStem(), q.getDimension(), opts);
        }).toList();

        return new StartResult(views.size(), mode, views);
    }

    /** 提交：评分 + 维度短板 + 画像 + 高危场景 + 建议，并持久化记录。 */
    public SubmitResult submit(SubmitRequest req) {
        List<Long> qids = req.answers().stream()
                .map(SubmitRequest.Answer::questionId).distinct().toList();

        Map<Long, String> qDim = questionRepo.findAllById(qids).stream()
                .collect(Collectors.toMap(QuizQuestion::getId, QuizQuestion::getDimension));
        Map<Long, List<QuizOption>> optMap = optionRepo.findByQuestionIdIn(qids).stream()
                .collect(Collectors.groupingBy(QuizOption::getQuestionId));
        Map<Long, Long> chosen = req.answers().stream()
                .collect(Collectors.toMap(SubmitRequest.Answer::questionId,
                        SubmitRequest.Answer::optionId, (a, b) -> a));

        Map<String, int[]> dimAgg = new LinkedHashMap<>();   // dim -> [correct,total]
        int correct = 0, total = 0;

        for (Long qid : qids) {
            String dim = qDim.get(qid);
            if (dim == null) continue;
            Long correctOptId = optMap.getOrDefault(qid, List.of()).stream()
                    .filter(o -> Boolean.TRUE.equals(o.getIsCorrect()))
                    .map(QuizOption::getId).findFirst().orElse(null);
            boolean ok = correctOptId != null && correctOptId.equals(chosen.get(qid));
            int[] agg = dimAgg.computeIfAbsent(dim, k -> new int[2]);
            agg[1]++;
            total++;
            if (ok) { agg[0]++; correct++; }
        }
        if (total == 0) throw new IllegalArgumentException("没有有效作答");

        int score = (int) Math.round(correct * 100.0 / total);

        List<DimScore> dimScores = dimAgg.entrySet().stream()
                .map(e -> new DimScore(e.getKey(), e.getValue()[0], e.getValue()[1],
                        (int) Math.round(e.getValue()[0] * 100.0 / e.getValue()[1])))
                .sorted(Comparator.comparing(DimScore::dimension))
                .toList();

        // 最弱维度排序（正确率升序）
        List<DimScore> weak = dimScores.stream()
                .sorted(Comparator.comparingInt(DimScore::rate))
                .toList();

        List<String> profileTags = new ArrayList<>();
        List<String> riskScenes = new ArrayList<>();
        List<String> advice = new ArrayList<>();
        for (DimScore d : weak) {
            String[] meta = DIM_META.get(d.dimension());
            if (meta == null) continue;
            if (profileTags.size() < 2) profileTags.add(meta[0]);
            if (riskScenes.size() < 3) riskScenes.add(meta[1]);
            if (advice.size() < 3) advice.add(meta[2]);
        }
        advice.add("遇事不决，先用「坑查查」查一查、问一问 AI 避坑助手");

        String profileTitle;
        if (score >= 80) {
            profileTitle = "人间清醒种子选手";
            if (profileTags.isEmpty()) profileTags.add("人间清醒");
        } else {
            profileTitle = String.join(" · ", profileTags.isEmpty()
                    ? List.of("待提升的避坑新手") : profileTags);
        }

        persist(req, score, dimScores, profileTags, riskScenes, advice);

        return new SubmitResult(score, correct, total, profileTitle,
                profileTags, dimScores, riskScenes, advice);
    }

    private void persist(SubmitRequest req, int score, List<DimScore> dimScores,
                         List<String> profileTags, List<String> riskScenes, List<String> advice) {
        try {
            TestRecord r = new TestRecord();
            r.setScale(req.scale());
            r.setMode(req.mode());
            r.setScore(score);
            r.setDimScores(objectMapper.writeValueAsString(dimScores));
            r.setProfileTags(objectMapper.writeValueAsString(profileTags));
            r.setRiskScenes(objectMapper.writeValueAsString(riskScenes));
            r.setAdvice(objectMapper.writeValueAsString(advice));
            r.setCreatedAt(LocalDateTime.now());
            recordRepo.save(r);
        } catch (Exception ignored) {
            // 持久化失败不影响返回结果
        }
    }
}
