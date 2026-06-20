package com.kengchacha.toolbox;

import com.kengchacha.common.ApiResponse;
import com.kengchacha.common.PageResult;
import com.kengchacha.toolbox.dto.DetectRequest;
import com.kengchacha.toolbox.dto.DetectResult;
import com.kengchacha.toolbox.dto.DetectionRecordView;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 风险检测工具箱：合同体检 / 链接验毒 / 拍照识坑 / 收款核验。
 * 多模态前置（OCR/抓取快照）由前端或网关转文本后传入；本服务做规则研判。
 */
@RestController
@RequestMapping("/api/toolbox")
public class ToolboxController {

    private final ToolboxService toolboxService;

    public ToolboxController(ToolboxService toolboxService) {
        this.toolboxService = toolboxService;
    }

    /** 合同体检：input=合同正文。 */
    @PostMapping("/contract")
    public ApiResponse<DetectResult> contract(@Valid @RequestBody DetectRequest req) {
        return ApiResponse.ok(toolboxService.contract(req.input()));
    }

    /** 链接验毒：input=待检测 URL。 */
    @PostMapping("/link")
    public ApiResponse<DetectResult> link(@Valid @RequestBody DetectRequest req) {
        return ApiResponse.ok(toolboxService.link(req.input()));
    }

    /** 拍照识坑：input=截图/对话的 OCR 文本（前端 OCR 后传入）。 */
    @PostMapping("/image")
    public ApiResponse<DetectResult> image(@Valid @RequestBody DetectRequest req) {
        return ApiResponse.ok(toolboxService.image(req.input()));
    }

    /** 收款核验：input=收款账号/二维码信息，hint=对方自称名称/商家名。 */
    @PostMapping("/payee")
    public ApiResponse<DetectResult> payee(@Valid @RequestBody DetectRequest req) {
        return ApiResponse.ok(toolboxService.payee(req.input(), req.hint()));
    }

    /** 检测记录（私密）：仅返回脱敏预览与结论。 */
    @GetMapping("/records")
    public ApiResponse<PageResult<DetectionRecordView>> records(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(toolboxService.records(page, size));
    }
}
