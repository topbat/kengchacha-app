package com.kengchacha.assistant;

import com.kengchacha.assistant.dto.ChatAnswer;
import com.kengchacha.assistant.dto.ChatRequest;
import com.kengchacha.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assistant")
public class AssistantController {

    private final AssistantService assistantService;

    public AssistantController(AssistantService assistantService) {
        this.assistantService = assistantService;
    }

    /** 对话查询（文字/语音转写/拍照转写/链接 由前端转文本后传入 message）。 */
    @PostMapping("/chat")
    public ApiResponse<ChatAnswer> chat(@Valid @RequestBody ChatRequest req) {
        return ApiResponse.ok(assistantService.chat(req.message(), req.inputType()));
    }
}
