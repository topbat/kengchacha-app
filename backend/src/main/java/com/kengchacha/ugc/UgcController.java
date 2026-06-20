package com.kengchacha.ugc;

import com.kengchacha.common.ApiResponse;
import com.kengchacha.common.PageResult;
import com.kengchacha.ugc.dto.CreateStoryRequest;
import com.kengchacha.ugc.dto.StoryView;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ugc")
public class UgcController {

    private final UgcService ugcService;

    public UgcController(UgcService ugcService) {
        this.ugcService = ugcService;
    }

    /** 踩坑广场列表（仅审核通过）。 */
    @GetMapping("/stories")
    public ApiResponse<PageResult<StoryView>> stories(@RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(ugcService.list(page, size));
    }

    /** 上报踩坑经历。 */
    @PostMapping("/stories")
    public ApiResponse<StoryView> create(@Valid @RequestBody CreateStoryRequest req) {
        return ApiResponse.ok(ugcService.create(req));
    }

    /** 点赞：type=learn / pity。 */
    @PostMapping("/stories/{id}/like")
    public ApiResponse<StoryView> like(@PathVariable Long id,
                                       @RequestParam(defaultValue = "learn") String type) {
        return ApiResponse.ok(ugcService.like(id, type));
    }
}
