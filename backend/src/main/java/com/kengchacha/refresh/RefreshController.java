package com.kengchacha.refresh;

import com.kengchacha.common.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 内容池"每 2 小时滚动更新"的倒计时。
 * 下次更新对齐到下一个 interval 整点边界，前后端一致、无状态（对齐 PRD §5.1 / 技术方案 §9）。
 */
@RestController
@RequestMapping("/api/refresh")
public class RefreshController {

    @Value("${kengchacha.refresh.interval-hours:2}")
    private int intervalHours;

    public record CountdownView(long secondsLeft, String nextAt, int intervalHours) {
    }

    @GetMapping("/countdown")
    public ApiResponse<CountdownView> countdown() {
        long interval = intervalHours * 3600L;
        long now = Instant.now().getEpochSecond();
        long next = ((now / interval) + 1) * interval;
        String nextAt = ZonedDateTime.ofInstant(Instant.ofEpochSecond(next), ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return ApiResponse.ok(new CountdownView(next - now, nextAt, intervalHours));
    }
}
