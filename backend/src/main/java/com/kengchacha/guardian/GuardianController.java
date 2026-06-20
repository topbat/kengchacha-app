package com.kengchacha.guardian;

import com.kengchacha.common.ApiResponse;
import com.kengchacha.guardian.dto.AlertView;
import com.kengchacha.guardian.dto.BindRequest;
import com.kengchacha.guardian.dto.GuardianOverview;
import com.kengchacha.guardian.dto.RelationView;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 家人守护：关系绑定、订阅、预警推送与已读。 */
@RestController
@RequestMapping("/api/guardian")
public class GuardianController {

    private final GuardianService guardianService;

    public GuardianController(GuardianService guardianService) {
        this.guardianService = guardianService;
    }

    /** 守护中心概览（关系 + 预警 + 未读数）。 */
    @GetMapping("/overview")
    public ApiResponse<GuardianOverview> overview() {
        return ApiResponse.ok(guardianService.overview());
    }

    /** 绑定家人并订阅风险领域。 */
    @PostMapping("/relations")
    public ApiResponse<RelationView> bind(@Valid @RequestBody BindRequest req) {
        return ApiResponse.ok(guardianService.bind(req));
    }

    @GetMapping("/relations")
    public ApiResponse<List<RelationView>> relations() {
        return ApiResponse.ok(guardianService.relations());
    }

    @DeleteMapping("/relations/{id}")
    public ApiResponse<Void> unbind(@PathVariable Long id) {
        guardianService.unbind(id);
        return ApiResponse.ok();
    }

    /** 为某家人扫描并推送新高危预警。 */
    @PostMapping("/relations/{id}/push")
    public ApiResponse<List<AlertView>> push(@PathVariable Long id) {
        return ApiResponse.ok(guardianService.push(id));
    }

    /** 一键守护：为所有家人扫描并推送。 */
    @PostMapping("/push-all")
    public ApiResponse<List<AlertView>> pushAll() {
        return ApiResponse.ok(guardianService.pushAll());
    }

    @GetMapping("/alerts")
    public ApiResponse<List<AlertView>> alerts() {
        return ApiResponse.ok(guardianService.alerts());
    }

    @PostMapping("/alerts/{id}/read")
    public ApiResponse<Void> read(@PathVariable Long id) {
        guardianService.markRead(id);
        return ApiResponse.ok();
    }
}
