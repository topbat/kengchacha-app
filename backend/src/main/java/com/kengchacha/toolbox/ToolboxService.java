package com.kengchacha.toolbox;

import com.kengchacha.common.PageResult;
import com.kengchacha.toolbox.dto.DetectResult;
import com.kengchacha.toolbox.dto.DetectionRecordView;
import com.kengchacha.toolbox.dto.RiskHit;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 风险检测工具箱（离线规则版，确定性、不联网）。
 *
 * 对齐技术方案 §7.4：
 *  ① 合同体检：条款切分 → 逐条对照「风险条款知识库」→ 标红 + 解释 + 建议；
 *  ② 链接验毒：域名信誉库 + 仿冒检测（相似域名/品牌）+ 可疑特征研判 → 风险等级；
 *  ③ 拍照识坑：危险话术分类（先付款/走私户/屏幕共享/征信修复…）→ 命中套路 + 高亮；
 *  ④ 收款核验：对私账户、户名不符、敏感备注等启发式校验。
 *
 * 生产可把规则库换成「知识库 + LLM 逐条研判 + OCR/图像理解」，对外接口不变。
 */
@Service
public class ToolboxService {

    public static final Map<Integer, String> TOOL_NAMES = Map.of(
            1, "合同体检", 2, "链接验毒", 3, "拍照识坑", 4, "收款核验");

    private static final String DISCLAIMER =
            "AI 辅助初筛，不构成法律或官方结论；如涉重大权益请咨询专业人士或拨打 96110 / 12315 核实。";

    private final DetectionRecordRepository recordRepository;

    public ToolboxService(DetectionRecordRepository recordRepository) {
        this.recordRepository = recordRepository;
    }

    // ============================================================
    // 1) 合同体检
    // ============================================================

    /** 风险条款知识库：关键词命中即提示（演进可换成条款切分 + LLM 逐条对照）。 */
    private static final ClausePattern[] CONTRACT_RULES = {
            new ClausePattern(List.of("概不退还", "不予退还", "定金不退", "一经售出", "不退不换", "恕不退款"),
                    "定金/费用不退条款", "约定预付款项一律不退，消费者冷静期与退订权被剥夺。",
                    "核对是否有“7 天冷静期/合理退订”条款；大额预付建议分期、留存凭证。", 3),
            new ClausePattern(List.of("最终解释权", "保留最终解释"),
                    "“最终解释权归本公司”", "属于经营者单方扩权的无效格式条款，常用来事后变卦。",
                    "该条款依法无效，可要求删除；遇纠纷向 12315 投诉。", 2),
            new ClausePattern(List.of("自动续费", "到期自动扣款", "自动展期", "默认续订"),
                    "自动续费/自动扣款", "未显著提示的自动续费易造成持续扣款、退订困难。",
                    "确认续费提醒与一键退订入口；关闭免密代扣。", 2),
            new ClausePattern(List.of("违约金", "违约责任"),
                    "高额违约金", "若违约金畸高（远超实际损失）显失公平，是常见“锁客”手段。",
                    "对照违约金比例是否合理（一般不超过实际损失 30%），过高可主张调整。", 2),
            new ClausePattern(List.of("空白", "另行约定", "以实际为准", "以最终为准", "详见附件"),
                    "空白/待定条款", "金额、范围、期限留空或“另行约定”，签后易被单方面填写。",
                    "所有金额、范围、期限当场写死；不接受空白处签字。", 3),
            new ClausePattern(List.of("竞业限制", "竞业禁止"),
                    "竞业限制（注意补偿）", "竞业限制须约定经济补偿，否则对劳动者不公平。",
                    "确认是否约定竞业补偿金及标准；无补偿可主张条款不生效。", 2),
            new ClausePattern(List.of("有权随时", "单方面", "本公司有权调整", "有权变更", "最终决定权"),
                    "单方变更/解释权", "赋予一方随意调整价格、规则、解约的权力，破坏对等。",
                    "要求改为“双方协商一致”；保留原始合同版本。", 2),
            new ClausePattern(List.of("贷款", "分期", "培训贷", "消费分期", "网贷"),
                    "培训/消费贷捆绑", "“先培训后上岗/先消费后还款”常捆绑贷款，背上真实债务。",
                    "看清是否在签贷款合同；入职/服务不应由你贷款付费。", 3),
            new ClausePattern(List.of("保证金", "押金", "服装费", "建档费", "工本费"),
                    "入职/服务前收费", "正规招聘与服务不会在提供前收取保证金、押金、服装费。",
                    "先收钱的一律警惕；不转账、不交押金。", 3),
    };

    public DetectResult contract(String text) {
        List<RiskHit> hits = new ArrayList<>();
        for (ClausePattern r : CONTRACT_RULES) {
            String hit = r.firstHit(text);
            if (hit != null) {
                hits.add(new RiskHit(r.label, snippetAround(text, hit), r.detail, r.advice, r.severity));
            }
        }
        DetectResult result = summarize(1, hits,
                "未发现明显风险条款，但仍建议逐条核对金额、期限与退订/违约条款。",
                "合同含多处需警惕的条款，签字前请逐条确认。");
        record(1, text, result);
        return result;
    }

    // ============================================================
    // 2) 链接验毒
    // ============================================================

    /** 可信主域（白名单，命中视为正常域名）。 */
    private static final List<String> TRUSTED = List.of(
            "gov.cn", "12315.cn", "96110.com", "taobao.com", "tmall.com", "jd.com",
            "alipay.com", "weixin.qq.com", "qq.com", "icbc.com.cn", "abchina.com",
            "ccb.com", "boc.cn", "unionpay.com", "12306.cn", "baidu.com");

    /** 常被仿冒的品牌主域（用于近似域名比对）。 */
    private static final List<String> BRANDS = List.of(
            "taobao", "tmall", "jd", "alipay", "weixin", "icbc", "ccb", "unionpay", "12306", "baidu");

    /** 域名/路径中的高危关键词。 */
    private static final List<String> URL_RED = List.of(
            "安全账户", "unfreeze", "jiedong", "解冻", "shouquan", "授权", "kefu", "客服",
            "verify", "validate", "active", "fanli", "返利", "lottery", "中奖", "winner");

    public DetectResult link(String raw) {
        String url = raw == null ? "" : raw.trim();
        List<RiskHit> hits = new ArrayList<>();
        String host = hostOf(url);

        boolean trusted = !host.isEmpty() && TRUSTED.stream().anyMatch(d -> host.equals(d) || host.endsWith("." + d));

        if (!url.toLowerCase().startsWith("https")) {
            hits.add(new RiskHit("非 HTTPS 明文链接", url, "未加密传输，账号密码可能被窃听。",
                    "正规支付/登录页均为 https；谨慎在 http 页面输入信息。", 1));
        }
        if (host.matches("\\d{1,3}(\\.\\d{1,3}){3}")) {
            hits.add(new RiskHit("IP 直连地址", host, "用 IP 代替域名是钓鱼站点常见特征。",
                    "正规机构不会让你访问纯数字地址。", 3));
        }
        if (url.contains("@")) {
            hits.add(new RiskHit("链接含 @ 跳转", url, "@ 前为伪装、真实目的地在 @ 之后，常用于伪装可信域名。",
                    "看清 @ 之后的真实域名再决定是否打开。", 3));
        }
        if (host.startsWith("xn--") || host.contains(".xn--")) {
            hits.add(new RiskHit("Punycode 国际化域名", host, "可用形似字符伪装成知名品牌域名。",
                    "对照官方 App 内的官方域名，不要从链接直接进入。", 3));
        }
        if (!trusted && host.chars().filter(c -> c == '.').count() >= 3) {
            hits.add(new RiskHit("多级子域名", host, "形如 brand.safe-login.xxx.cn，真实主域被藏在后面。",
                    "认准最后两段主域；前面的子域可随意伪造。", 2));
        }
        // 品牌近似仿冒：逐个域名片段（子域/主域，按 . 与 - 切分）与知名品牌比对，
        // 命中“包含/形近（0↔o、1↔l 等编辑距离1）/在非官方域名上原样出现”即判仿冒钓鱼。
        if (!trusted) {
            String brand = lookalikeBrand(host);
            if (brand != null) {
                hits.add(new RiskHit("疑似仿冒「" + brand + "」", host,
                        "域名（含子域）与知名品牌高度相似或盗用其名，如 0/o、1/l 替换、加“-安全/-login”后缀，属山寨钓鱼。",
                        "通过官方 App 或收藏夹访问，不要点陌生链接里的“官网”。", 3));
            }
        }
        String low = url.toLowerCase();
        for (String kw : URL_RED) {
            if (low.contains(kw.toLowerCase())) {
                hits.add(new RiskHit("可疑关键词：" + kw, kw,
                        "链接含“解冻/安全账户/返利/中奖/授权”等诱导字样。",
                        "凡引导“转账解冻、领奖、授权”的链接一律不点。", 2));
                break;
            }
        }

        String okMsg = trusted ? "命中可信主域白名单，但仍请核对完整域名后再操作。"
                : "未发现明显钓鱼特征，但陌生链接仍建议核实来源后再打开。";
        DetectResult result = summarize(2, hits, okMsg, "该链接存在钓鱼/仿冒特征，强烈建议不要打开或输入信息。");
        record(2, url, result);
        return result;
    }

    // ============================================================
    // 3) 拍照识坑（对截图/对话的 OCR 文本做危险话术分类）
    // ============================================================

    private static final ClausePattern[] PHRASE_RULES = {
            new ClausePattern(List.of("安全账户", "资金清查", "涉嫌洗钱", "通缉", "逮捕令"),
                    "冒充公检法", "世上没有“安全账户”，公检法不会电话/网络办案要你转账。",
                    "立即挂断，拨 96110 或 110 核实。", 3),
            new ClausePattern(List.of("先付款", "先转账", "先交", "押金", "保证金", "会员费", "解冻金", "刷单"),
                    "先收钱套路", "兼职/中奖/客服让你“先付款再返还”，付完即被拉黑。",
                    "任何要求你先付钱的都极可能是诈骗，停止转账。", 3),
            new ClausePattern(List.of("走私人账户", "私人账户", "对私", "扫码转我", "加我微信付款", "二维码付款"),
                    "走私户/私下收款", "绕开平台担保走私人账户，出事难追、无凭证。",
                    "走平台官方渠道支付；拒绝扫码到个人。", 3),
            new ClausePattern(List.of("屏幕共享", "共享屏幕", "下载会议", "远程协助", "视频会议"),
                    "屏幕共享/远程操控", "开启共享后验证码、银行页面会被对方看到甚至操控。",
                    "任何人让你开屏幕共享/装远程软件，立刻拒绝。", 3),
            new ClausePattern(List.of("征信修复", "修复征信", "消除逾期", "洗白征信", "注销网贷", "注销账户"),
                    "征信修复/注销网贷骗局", "征信不可花钱修复；“注销账户影响征信”是经典话术。",
                    "征信异议走官方渠道；挂断并拨 96110。", 3),
            new ClausePattern(List.of("稳赚", "保本", "高收益", "内部消息", "原始股", "带单", "导师", "跟单"),
                    "高收益投资诱导", "承诺稳赚高收益、拉群带单、站外 App 投资均为杀猪盘红线。",
                    "凡引导到站外 App、承诺稳赚的“投资”都是诈骗。", 3),
            new ClausePattern(List.of("验证码", "短信验证码", "动态码", "报给我"),
                    "索要验证码", "验证码等于你的钱包钥匙，给出即可被转走资金。",
                    "验证码绝不告诉任何人，包括“客服/警察”。", 3),
            new ClausePattern(List.of("免费送", "免费领", "免费体检", "免费讲座", "中奖", "返现"),
                    "小恩小惠诱导", "免费礼品/中奖是为了后续高价推销或套取信息。",
                    "天上不会掉馅饼；不贪小利、不留隐私。", 2),
    };

    public DetectResult image(String ocrText) {
        List<RiskHit> hits = new ArrayList<>();
        for (ClausePattern r : PHRASE_RULES) {
            String hit = r.firstHit(ocrText);
            if (hit != null) {
                hits.add(new RiskHit(r.label, hit, r.detail, r.advice, r.severity));
            }
        }
        DetectResult result = summarize(3, hits,
                "未识别到典型诈骗话术，但仍请提高警惕、核实对方身份。",
                "截图中出现多处典型诈骗话术，请立即停止操作并核实。");
        record(3, ocrText, result);
        return result;
    }

    // ============================================================
    // 4) 收款核验
    // ============================================================

    public DetectResult payee(String account, String claimName) {
        String acc = account == null ? "" : account.trim();
        String name = claimName == null ? "" : claimName.trim();
        List<RiskHit> hits = new ArrayList<>();

        boolean officialClaim = containsAny(name, List.of("公司", "官方", "平台", "客服", "银行", "政府", "机构", "旗舰店"));
        boolean privateAcc = containsAny(acc, List.of("微信", "支付宝", "个人", "wx", "zfb", "qq"))
                || acc.matches(".*1[3-9]\\d{9}.*");   // 手机号收款多为个人

        if (officialClaim && privateAcc) {
            hits.add(new RiskHit("公对私收款", acc,
                    "对方自称“" + name + "”却用个人微信/支付宝/手机号收款，公司收款应走对公账户。",
                    "要求对公账户与合同发票；拒绝转入个人账户。", 3));
        }
        if (containsAny(acc, List.of("安全账户", "保证金", "解冻", "验资", "刷单"))) {
            hits.add(new RiskHit("敏感收款用途", acc,
                    "收款备注/名义含“安全账户/保证金/解冻/验资”等典型诈骗用途。",
                    "立即停止转账，拨 96110 核实。", 3));
        }
        if (!name.isEmpty() && !acc.isEmpty() && privateAcc && !officialClaim
                && !acc.contains(name) && name.length() >= 2) {
            hits.add(new RiskHit("户名与对方不一致", acc + " / " + name,
                    "收款户名与你交易的对象不一致，可能是第三方“跑分”账户。",
                    "确认收款户名是否为本人/商家本身；不一致勿转。", 2));
        }
        if (acc.contains("二维码") || acc.contains("扫码")) {
            hits.add(new RiskHit("扫码到个人", acc,
                    "陌生二维码可能指向个人收款或木马页面。",
                    "走平台担保交易，不扫陌生码大额付款。", 2));
        }

        DetectResult result = summarize(4, hits,
                "未发现明显异常，但大额收款仍建议走平台担保、核对对公账户。",
                "收款方存在高风险特征，付款前务必再次核实。");
        record(4, acc + " " + name, result);
        return result;
    }

    // ============================================================
    // 检测记录（私密）
    // ============================================================

    public PageResult<DetectionRecordView> records(int page, int size) {
        List<DetectionRecord> rows = recordRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
        long total = recordRepository.count();
        List<DetectionRecordView> items = rows.stream()
                .map(r -> new DetectionRecordView(r.getId(), r.getToolType(),
                        TOOL_NAMES.getOrDefault(r.getToolType(), "检测"),
                        r.getPreview(), r.getRiskLevel(), r.getScore(), r.getCreatedAt()))
                .toList();
        return new PageResult<>(items, total, page, size);
    }

    // ============================================================
    // 公共：打分 / 落库 / 文本工具
    // ============================================================

    private DetectResult summarize(int toolType, List<RiskHit> hits, String okSummary, String riskSummary) {
        int maxSev = hits.stream().mapToInt(RiskHit::severity).max().orElse(0);
        int high = (int) hits.stream().filter(h -> h.severity() == 3).count();
        int riskLevel = hits.isEmpty() ? 1 : maxSev;
        // 风险分：高危条 ×30 + 其余 ×12，封顶 98；无命中给安全基线分 8。
        int score = hits.isEmpty() ? 8
                : Math.min(98, high * 30 + (hits.size() - high) * 12 + 10);
        String verdict = switch (riskLevel) {
            case 3 -> "⛔ 高风险";
            case 2 -> "⚠ 需注意";
            default -> "✅ 暂未发现明显风险";
        };
        List<String> advice = hits.isEmpty()
                ? List.of("保持警惕，遇“先转账/给验证码/走私户”一律拒绝。")
                : hits.stream().map(RiskHit::advice).distinct().limit(4).toList();
        String summary = hits.isEmpty() ? okSummary
                : riskSummary + "（命中 " + hits.size() + " 项，其中高危 " + high + " 项）";
        return new DetectResult(toolType, riskLevel, score, verdict, summary, hits, advice, DISCLAIMER);
    }

    private void record(int toolType, String input, DetectResult r) {
        DetectionRecord rec = new DetectionRecord();
        rec.setToolType(toolType);
        rec.setPreview(mask(input));
        rec.setRiskLevel(r.riskLevel());
        rec.setScore(r.score());
        rec.setCreatedAt(LocalDateTime.now());
        recordRepository.save(rec);
    }

    /** 脱敏预览：截断到 28 字，对中间做打码（隐私最小化）。 */
    private static String mask(String s) {
        String t = (s == null ? "" : s).replaceAll("\\s+", " ").trim();
        if (t.length() > 28) t = t.substring(0, 28) + "…";
        if (t.length() <= 6) return t;
        int keep = 3;
        return t.substring(0, keep) + "***" + t.substring(t.length() - keep);
    }

    private static String snippetAround(String text, String kw) {
        if (text == null) return kw;
        int i = text.indexOf(kw);
        if (i < 0) return kw;
        int from = Math.max(0, i - 8), to = Math.min(text.length(), i + kw.length() + 8);
        String s = text.substring(from, to).replaceAll("\\s+", " ");
        return (from > 0 ? "…" : "") + s + (to < text.length() ? "…" : "");
    }

    private static String hostOf(String url) {
        String u = url == null ? "" : url.trim();
        u = u.replaceFirst("(?i)^[a-z]+://", "");
        int at = u.indexOf('@');
        if (at >= 0) u = u.substring(at + 1);
        int slash = u.indexOf('/');
        if (slash >= 0) u = u.substring(0, slash);
        int colon = u.indexOf(':');
        if (colon >= 0) u = u.substring(0, colon);
        return u.toLowerCase();
    }

    /** 在域名各片段（按 . 与 - 切分）中找出被仿冒的知名品牌；无则 null。 */
    private static String lookalikeBrand(String host) {
        if (host == null || host.isEmpty()) return null;
        java.util.LinkedHashSet<String> tokens = new java.util.LinkedHashSet<>();
        for (String part : host.split("[.-]")) {
            if (!part.isEmpty() && !part.equals("cn") && !part.equals("com") && !part.equals("www")) tokens.add(part);
        }
        for (String tok : tokens) {
            for (String b : BRANDS) {
                if (b.length() < 3) continue;                                   // 过短品牌(jd)易误报
                if (tok.equals(b)) return b;                                    // 非官方域名却原样用品牌名
                if (tok.length() > b.length() && tok.contains(b)) return b;     // 品牌名+后缀（taobao-anquan）
                if (Math.abs(tok.length() - b.length()) <= 1 && levenshtein(tok, b) == 1) return b; // 形近(0↔o,1↔l)
            }
        }
        return null;
    }

    private static boolean containsAny(String s, List<String> keys) {
        if (s == null) return false;
        String low = s.toLowerCase();
        return keys.stream().anyMatch(k -> low.contains(k.toLowerCase()));
    }

    private static int levenshtein(String a, String b) {
        int[] prev = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) prev[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            int[] cur = new int[b.length() + 1];
            cur[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                cur[j] = Math.min(Math.min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            prev = cur;
        }
        return prev[b.length()];
    }

    /** 规则模式：任一关键词命中即触发。 */
    private record ClausePattern(List<String> keywords, String label, String detail, String advice, int severity) {
        String firstHit(String text) {
            if (text == null) return null;
            for (String k : keywords) if (text.contains(k)) return k;
            return null;
        }
    }
}
