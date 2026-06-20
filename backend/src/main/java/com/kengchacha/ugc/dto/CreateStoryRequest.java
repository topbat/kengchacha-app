package com.kengchacha.ugc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateStoryRequest(
        String nickname,
        String happenTime,
        String region,
        String groupTag,
        String domainTag,
        @NotBlank(message = "事件经过不能为空")
        @Size(min = 5, max = 2000, message = "事件经过 5-2000 字")
        String content,
        String advice
) {
}
