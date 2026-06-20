package com.kengchacha.growth;

import com.kengchacha.common.ApiResponse;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 成长体系（MVP 演示版）：徽章字典 + 我的成长概览。
 * 真实登录态接入后，contribScore 等由 user_growth 表驱动。
 */
@RestController
@RequestMapping("/api/growth")
public class GrowthController {

    private final BadgeRepository badgeRepository;

    public GrowthController(BadgeRepository badgeRepository) {
        this.badgeRepository = badgeRepository;
    }

    public record BadgeView(Long id, String name, String icon, Integer level,
                            Integer needScore, boolean owned) {
    }

    public record MyGrowth(String nickname, int contribScore, int level, String currentBadge,
                           int validStoryCnt, List<BadgeView> badges) {
    }

    @GetMapping("/me")
    public ApiResponse<MyGrowth> me() {
        int contrib = 1280;   // 演示数据
        List<BadgeView> badges = badgeRepository.findAll(Sort.by("needScore")).stream()
                .map(b -> new BadgeView(b.getId(), b.getName(), b.getIcon(), b.getLevel(),
                        b.getNeedScore(), contrib >= b.getNeedScore()))
                .toList();
        return ApiResponse.ok(new MyGrowth("清醒的打工人", contrib, 3,
                "人间清醒·LV3", 14, badges));
    }
}
