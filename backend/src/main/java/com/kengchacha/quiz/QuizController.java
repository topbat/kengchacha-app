package com.kengchacha.quiz;

import com.kengchacha.common.ApiResponse;
import com.kengchacha.quiz.dto.StartResult;
import com.kengchacha.quiz.dto.SubmitRequest;
import com.kengchacha.quiz.dto.SubmitResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/quiz")
public class QuizController {

    private final QuizService quizService;

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    /** 出题：scale=10/30/50，mode=1随机 2针对 3自适应（MVP 统一随机）。 */
    @GetMapping("/start")
    public ApiResponse<StartResult> start(@RequestParam(defaultValue = "10") int scale,
                                          @RequestParam(defaultValue = "1") int mode) {
        return ApiResponse.ok(quizService.start(scale, mode));
    }

    /** 提交答卷，返回画像与建议。 */
    @PostMapping("/submit")
    public ApiResponse<SubmitResult> submit(@Valid @RequestBody SubmitRequest req) {
        return ApiResponse.ok(quizService.submit(req));
    }
}
