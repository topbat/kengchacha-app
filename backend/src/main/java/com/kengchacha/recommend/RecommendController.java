package com.kengchacha.recommend;

import com.kengchacha.common.ApiResponse;
import com.kengchacha.recommend.dto.RecItem;
import com.kengchacha.recommend.dto.RecommendRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 向量检索 / 个性化推荐。
 *  - 相似案例：/api/recommend/similar/{id}
 *  - 为你推荐：/api/recommend/for-you（兴趣标签 + 近期浏览）
 */
@RestController
@RequestMapping("/api/recommend")
public class RecommendController {

    private final RecommendService recommendService;

    public RecommendController(RecommendService recommendService) {
        this.recommendService = recommendService;
    }

    /** 相似案例（语义近邻，排除自身）。 */
    @GetMapping("/similar/{id}")
    public ApiResponse<List<RecItem>> similar(@PathVariable Long id,
                                              @RequestParam(defaultValue = "5") int size) {
        return ApiResponse.ok(recommendService.similar(id, size));
    }

    /** 为你推荐：基于兴趣向量召回。无兴趣无浏览时回退热门高危。 */
    @PostMapping("/for-you")
    public ApiResponse<List<RecItem>> forYou(@RequestBody(required = false) RecommendRequest req) {
        return ApiResponse.ok(recommendService.forYou(req));
    }
}
