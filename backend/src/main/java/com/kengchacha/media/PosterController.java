package com.kengchacha.media;

import com.kengchacha.common.ApiResponse;
import com.kengchacha.media.dto.PosterRequest;
import com.kengchacha.media.dto.PosterView;
import org.springframework.web.bind.annotation.*;

/** 文生图海报：生成可分享的竖版海报（服务端 SVG，前端可下载/转 PNG）。 */
@RestController
@RequestMapping("/api/share")
public class PosterController {

    private final PosterService posterService;

    public PosterController(PosterService posterService) {
        this.posterService = posterService;
    }

    /** 生成分享海报：传 contentId 或自定义四要素。 */
    @PostMapping("/poster")
    public ApiResponse<PosterView> poster(@RequestBody PosterRequest req) {
        return ApiResponse.ok(posterService.render(req));
    }
}
