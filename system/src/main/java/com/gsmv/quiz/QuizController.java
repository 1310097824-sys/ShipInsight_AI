package com.gsmv.quiz;

import com.gsmv.common.ApiResponse;
import com.gsmv.common.PageResponse;
import com.gsmv.quiz.dto.QuizExamRequest;
import com.gsmv.quiz.dto.QuizExamStartResponse;
import com.gsmv.quiz.dto.QuizQuestionSaveRequest;
import com.gsmv.quiz.dto.QuizResultResponse;
import com.gsmv.quiz.dto.QuizSubmitRequest;
import com.gsmv.quiz.model.QuizQuestion;
import com.gsmv.quiz.model.QuizRecord;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/quiz")
public class QuizController {

    private final QuizService quizService;

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    // ==================== Question CRUD ====================

    @GetMapping("/questions")
    @PreAuthorize("hasAuthority('QUIZ_READ')")
    public ApiResponse<PageResponse<QuizQuestion>> listQuestions(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(quizService.listQuestions(category, type, difficulty, keyword, page, size));
    }

    @GetMapping("/questions/{id}")
    @PreAuthorize("hasAuthority('QUIZ_READ')")
    public ApiResponse<QuizQuestion> getQuestion(@PathVariable Long id) {
        return ApiResponse.success(quizService.getQuestion(id));
    }

    @PostMapping("/questions")
    @PreAuthorize("hasAuthority('QUIZ_WRITE')")
    public ApiResponse<QuizQuestion> createQuestion(@Valid @RequestBody QuizQuestionSaveRequest request) {
        return ApiResponse.success(quizService.createQuestion(request));
    }

    @PutMapping("/questions/{id}")
    @PreAuthorize("hasAuthority('QUIZ_WRITE')")
    public ApiResponse<QuizQuestion> updateQuestion(@PathVariable Long id,
                                                     @Valid @RequestBody QuizQuestionSaveRequest request) {
        return ApiResponse.success(quizService.updateQuestion(id, request));
    }

    @DeleteMapping("/questions/{id}")
    @PreAuthorize("hasAuthority('QUIZ_WRITE')")
    public ApiResponse<Void> deleteQuestion(@PathVariable Long id) {
        quizService.deleteQuestion(id);
        return ApiResponse.success(null);
    }

    @PutMapping("/questions/{id}/toggle")
    @PreAuthorize("hasAuthority('QUIZ_WRITE')")
    public ApiResponse<Void> toggleQuestion(@PathVariable Long id) {
        quizService.toggleQuestionStatus(id);
        return ApiResponse.success(null);
    }

    @GetMapping("/questions/stats")
    @PreAuthorize("hasAuthority('QUIZ_READ')")
    public ApiResponse<Map<String, Long>> questionStats() {
        return ApiResponse.success(Map.of(
                "ship", quizService.countByCategory("SHIP"),
                "weather", quizService.countByCategory("WEATHER"),
                "seaArea", quizService.countByCategory("SEA_AREA")
        ));
    }

    // ==================== Exam ====================

    @PostMapping("/exam/start")
    @PreAuthorize("hasAuthority('QUIZ_READ')")
    public ApiResponse<QuizExamStartResponse> startExam(@RequestBody QuizExamRequest request) {
        return ApiResponse.success(quizService.startExam(request));
    }

    @PostMapping("/exam/submit")
    @PreAuthorize("hasAuthority('QUIZ_READ')")
    public ApiResponse<QuizResultResponse> submitExam(@RequestBody QuizSubmitRequest request) {
        return ApiResponse.success(quizService.submitExam(request));
    }

    @GetMapping("/exam/result/{recordId}")
    @PreAuthorize("hasAuthority('QUIZ_READ')")
    public ApiResponse<QuizResultResponse> getResult(@PathVariable Long recordId) {
        return ApiResponse.success(quizService.getExamResult(recordId));
    }

    // ==================== History ====================

    @GetMapping("/records")
    @PreAuthorize("hasAuthority('QUIZ_READ')")
    public ApiResponse<PageResponse<QuizRecord>> listRecords(
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(quizService.listRecords(userId, page, size));
    }
}
