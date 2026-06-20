package com.kengchacha.media;

import com.kengchacha.content.Content;
import com.kengchacha.content.ContentRepository;
import com.kengchacha.media.dto.PosterRequest;
import com.kengchacha.media.dto.PosterView;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 文生图海报：结构化要素 → 模板引擎（固定版式：标题/套路/损失/口诀，重点高亮）
 * + 程序化生成的装饰背景（“AI 插画”占位）→ 合成竖版分享海报（服务端渲染 SVG，多端排版一致）。
 *
 * 生产可把背景换成通义万相/文心一格生成图，版式层（本类）不变；技术方案 §7.3。
 */
@Service
public class PosterService {

    private static final int W = 600, H = 900, PAD = 28;
    private static final int CX = 56, CR = 544, CW = 488;   // 内容左 / 右 / 宽

    private final ContentRepository contentRepository;
    private final MediaAssetRepository mediaAssetRepository;

    public PosterService(ContentRepository contentRepository, MediaAssetRepository mediaAssetRepository) {
        this.contentRepository = contentRepository;
        this.mediaAssetRepository = mediaAssetRepository;
    }

    public PosterView render(PosterRequest req) {
        // 取数：优先 contentId，缺字段用入参补
        String title = req.title(), trick = req.trick(), loss = req.loss(), tip = req.tip(), slogan = req.slogan();
        Integer hazard = req.hazardLevel();
        Long contentId = req.contentId();
        if (contentId != null) {
            Content c = contentRepository.findById(contentId).orElse(null);
            if (c != null) {
                title = orDefault(title, c.getTitle());
                trick = orDefault(trick, c.getTrick());
                loss = orDefault(loss, c.getLoss());
                tip = orDefault(tip, c.getTip());
                slogan = orDefault(slogan, c.getSlogan());
                if (hazard == null) hazard = c.getHazardLevel();
            }
        }
        title = orDefault(title, "防坑提醒");
        int hz = Optional.ofNullable(hazard).orElse(2);
        String theme = orDefault(req.theme(), "auto");

        String svg = buildSvg(title, trick, loss, tip, slogan, hz, theme);

        MediaAsset asset = new MediaAsset();
        asset.setContentId(contentId);
        asset.setMediaType(1);
        asset.setTitle(title);
        asset.setTheme(theme);
        asset.setCreatedAt(LocalDateTime.now());
        asset = mediaAssetRepository.save(asset);

        return new PosterView(asset.getId(), title, W, H, theme, svg);
    }

    // ============================================================
    // SVG 构建
    // ============================================================

    private String buildSvg(String title, String trick, String loss, String tip, String slogan,
                            int hazard, String theme) {
        String accent = switch (hazard) {           // 危害等级配色
            case 3 -> "#EF4444";
            case 1 -> "#22C55E";
            default -> "#F59E0B";
        };
        String hazardTxt = switch (hazard) {
            case 3 -> "高危预警";
            case 1 -> "低危提示";
            default -> "中危注意";
        };
        boolean dark = "dark".equalsIgnoreCase(theme);
        String panel = dark ? "#0F172A" : "#FFFFFF";
        String ink = dark ? "#F1F5F9" : "#0F172A";
        String ink2 = dark ? "#94A3B8" : "#475569";
        String chipBg = dark ? "#1E293B" : "#F0FBF8";

        StringBuilder s = new StringBuilder(4096);
        s.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"").append(W).append("\" height=\"").append(H)
                .append("\" viewBox=\"0 0 ").append(W).append(' ').append(H)
                .append("\" font-family=\"-apple-system,BlinkMacSystemFont,'PingFang SC','Microsoft YaHei',sans-serif\">");

        // ---- defs：背景渐变 + “插画”光斑 ----
        s.append("<defs>")
                .append("<linearGradient id=\"bg\" x1=\"0\" y1=\"0\" x2=\"1\" y2=\"1\">")
                .append("<stop offset=\"0\" stop-color=\"#0E9E8E\"/><stop offset=\"1\" stop-color=\"#06382F\"/>")
                .append("</linearGradient>")
                .append("<radialGradient id=\"glow\" cx=\"0.8\" cy=\"0.15\" r=\"0.6\">")
                .append("<stop offset=\"0\" stop-color=\"#16D6B4\" stop-opacity=\"0.55\"/><stop offset=\"1\" stop-color=\"#16D6B4\" stop-opacity=\"0\"/>")
                .append("</radialGradient>")
                .append("<linearGradient id=\"head\" x1=\"0\" y1=\"0\" x2=\"1\" y2=\"0\">")
                .append("<stop offset=\"0\" stop-color=\"#0E9E8E\"/><stop offset=\"1\" stop-color=\"#0B6E6B\"/>")
                .append("</linearGradient>")
                .append("</defs>");

        // ---- 程序化“文生图”背景 ----
        s.append("<rect width=\"").append(W).append("\" height=\"").append(H).append("\" fill=\"url(#bg)\"/>");
        s.append("<rect width=\"").append(W).append("\" height=\"").append(H).append("\" fill=\"url(#glow)\"/>");
        s.append("<circle cx=\"80\" cy=\"820\" r=\"160\" fill=\"#16D6B4\" opacity=\"0.10\"/>");
        s.append("<circle cx=\"520\" cy=\"700\" r=\"110\" fill=\"#FFFFFF\" opacity=\"0.06\"/>");
        s.append("<text x=\"").append(W - 36).append("\" y=\"").append(H - 28)
                .append("\" font-size=\"180\" text-anchor=\"end\" opacity=\"0.06\" fill=\"#FFFFFF\">🛡️</text>");

        // ---- 内容白板 ----
        s.append("<rect x=\"").append(PAD).append("\" y=\"").append(PAD)
                .append("\" rx=\"26\" width=\"").append(W - 2 * PAD).append("\" height=\"").append(H - 2 * PAD)
                .append("\" fill=\"").append(panel).append("\"/>");

        // 顶部品牌条
        s.append("<rect x=\"").append(PAD).append("\" y=\"").append(PAD)
                .append("\" rx=\"26\" width=\"").append(W - 2 * PAD).append("\" height=\"78\" fill=\"url(#head)\"/>");
        s.append("<rect x=\"").append(PAD).append("\" y=\"").append(PAD + 40)
                .append("\" width=\"").append(W - 2 * PAD).append("\" height=\"38\" fill=\"url(#head)\"/>");
        s.append(text("🛡️ 坑查查", CX, 78, "font-size=\"26\" font-weight=\"800\" fill=\"#FFFFFF\""));
        s.append(text("有坑没坑 · 先查查", CR, 78, "font-size=\"13\" text-anchor=\"end\" fill=\"#D6FBF3\""));

        int y = 132;
        // 危害徽章
        s.append("<rect x=\"").append(CX).append("\" y=\"").append(y - 18)
                .append("\" rx=\"12\" width=\"118\" height=\"30\" fill=\"").append(accent).append("\"/>");
        s.append(text("● " + hazardTxt, CX + 14, y + 3, "font-size=\"14\" font-weight=\"700\" fill=\"#FFFFFF\""));
        y += 46;

        // 标题（最多 3 行）
        List<String> titleLines = wrap(title, 13, 3);
        s.append(textBlock(titleLines, CX, y, 42, "font-size=\"33\" font-weight=\"800\" fill=\"" + ink + "\""));
        y += titleLines.size() * 42 + 14;

        // 分隔线
        s.append("<rect x=\"").append(CX).append("\" y=\"").append(y).append("\" width=\"").append(CW)
                .append("\" height=\"2\" fill=\"").append(dark ? "#1E293B" : "#E6EBF1").append("\"/>");
        y += 26;

        // 三要素
        y = element(s, "坑人套路", trick, y, accent, ink, ink2);
        y = element(s, "损失后果", loss, y, accent, ink, ink2);
        y = element(s, "避坑常识", tip, y, "#0B6E6B", ink, ink2);

        // 口诀高亮
        if (slogan != null && !slogan.isBlank()) {
            y += 6;
            List<String> sl = wrap("💡 " + slogan, 18, 2);
            int boxH = sl.size() * 30 + 22;
            s.append("<rect x=\"").append(CX).append("\" y=\"").append(y)
                    .append("\" rx=\"12\" width=\"").append(CW).append("\" height=\"").append(boxH)
                    .append("\" fill=\"").append(chipBg).append("\"/>");
            s.append("<rect x=\"").append(CX).append("\" y=\"").append(y)
                    .append("\" rx=\"12\" width=\"6\" height=\"").append(boxH).append("\" fill=\"#16D6B4\"/>");
            s.append(textBlock(sl, CX + 18, y + 30, 30, "font-size=\"18\" font-weight=\"700\" fill=\"#0B6E6B\""));
        }

        // 底部：反诈专线 + 仿二维码 + 免责
        int fy = H - PAD - 92;
        s.append("<rect x=\"").append(CX).append("\" y=\"").append(fy)
                .append("\" width=\"").append(CW).append("\" height=\"2\" fill=\"").append(dark ? "#1E293B" : "#E6EBF1").append("\"/>");
        s.append(text("反诈专线 96110", CX, fy + 34, "font-size=\"18\" font-weight=\"800\" fill=\"" + accent + "\""));
        s.append(text("消费维权 12315 · 法律援助 12348", CX, fy + 60, "font-size=\"13\" fill=\"" + ink2 + "\""));
        s.append(text("AI 科普改写示意 · 不构成法律/官方结论", CX, fy + 82, "font-size=\"11\" fill=\"" + ink2 + "\""));
        s.append(fakeQr(title, CR - 64, fy + 14, 64));

        s.append("</svg>");
        return s.toString();
    }

    /** 渲染一条「标签 + 内容（最多 2 行）」要素，返回新的 y 游标。 */
    private int element(StringBuilder s, String label, String value, int y, String accent, String ink, String ink2) {
        if (value == null || value.isBlank()) value = "—";
        s.append("<rect x=\"").append(CX).append("\" y=\"").append(y - 13)
                .append("\" rx=\"7\" width=\"").append(label.length() * 17 + 16).append("\" height=\"24\" fill=\"")
                .append(accent).append("\" opacity=\"0.14\"/>");
        s.append(text(label, CX + 8, y + 4, "font-size=\"14\" font-weight=\"700\" fill=\"" + accent + "\""));
        List<String> lines = wrap(value, 24, 2);
        s.append(textBlock(lines, CX, y + 32, 28, "font-size=\"17\" fill=\"" + ink + "\""));
        return y + 32 + (lines.size() - 1) * 28 + 24;
    }

    /** 由标题哈希生成的“仿二维码”装饰（6×6）。 */
    private String fakeQr(String seed, int x, int y, int size) {
        int n = 6, cell = size / n;
        long h = 1125899906842597L;
        for (int i = 0; i < seed.length(); i++) h = 31 * h + seed.charAt(i);
        StringBuilder sb = new StringBuilder();
        sb.append("<rect x=\"").append(x - 6).append("\" y=\"").append(y - 6)
                .append("\" rx=\"8\" width=\"").append(size + 12).append("\" height=\"").append(size + 12).append("\" fill=\"#FFFFFF\" stroke=\"#E6EBF1\"/>");
        for (int r = 0; r < n; r++) {
            for (int c = 0; c < n; c++) {
                boolean on = ((h >> ((r * n + c) % 63)) & 1L) == 1L
                        || r == 0 || c == 0 || r == n - 1 || c == n - 1;   // 加边框更像码
                if (on) {
                    sb.append("<rect x=\"").append(x + c * cell).append("\" y=\"").append(y + r * cell)
                            .append("\" width=\"").append(cell - 1).append("\" height=\"").append(cell - 1).append("\" fill=\"#0F172A\"/>");
                }
            }
        }
        return sb.toString();
    }

    // ---------- 文本工具 ----------

    private static String text(String t, int x, int y, String attrs) {
        return "<text x=\"" + x + "\" y=\"" + y + "\" " + attrs + ">" + esc(t) + "</text>";
    }

    private static String textBlock(List<String> lines, int x, int y, int lineH, String attrs) {
        StringBuilder sb = new StringBuilder("<text x=\"").append(x).append("\" y=\"").append(y).append("\" ").append(attrs).append(">");
        for (int i = 0; i < lines.size(); i++) {
            sb.append("<tspan x=\"").append(x).append("\" dy=\"").append(i == 0 ? 0 : lineH).append("\">")
                    .append(esc(lines.get(i))).append("</tspan>");
        }
        return sb.append("</text>").toString();
    }

    /** CJK 按 1、ASCII 按 0.55 计宽折行；最多 maxLines 行，超出末行省略号。 */
    private static List<String> wrap(String text, double maxUnits, int maxLines) {
        List<String> lines = new ArrayList<>();
        if (text == null) return lines;
        StringBuilder cur = new StringBuilder();
        double w = 0;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            double cw = ch >= 0x2E80 ? 1.0 : 0.55;
            if (w + cw > maxUnits && cur.length() > 0) {
                lines.add(cur.toString());
                cur.setLength(0);
                w = 0;
                if (lines.size() == maxLines) break;
            }
            cur.append(ch);
            w += cw;
        }
        if (lines.size() < maxLines && cur.length() > 0) lines.add(cur.toString());
        // 超长截断标记
        if (lines.size() == maxLines) {
            int consumed = lines.stream().mapToInt(String::length).sum();
            if (consumed < text.length()) {
                String last = lines.get(maxLines - 1);
                if (last.length() > 1) last = last.substring(0, last.length() - 1);
                lines.set(maxLines - 1, last + "…");
            }
        }
        return lines;
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static String orDefault(String v, String def) {
        return v == null || v.isBlank() ? def : v;
    }
}
