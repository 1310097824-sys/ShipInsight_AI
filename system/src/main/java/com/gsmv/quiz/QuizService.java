package com.gsmv.quiz;

import com.gsmv.common.ErrorCode;
import com.gsmv.common.PageResponse;
import com.gsmv.common.exception.BusinessException;
import com.gsmv.common.exception.NotFoundException;
import com.gsmv.quiz.dto.QuizExamRequest;
import com.gsmv.quiz.dto.QuizExamStartResponse;
import com.gsmv.quiz.dto.QuizQuestionSaveRequest;
import com.gsmv.quiz.dto.QuizResultResponse;
import com.gsmv.quiz.dto.QuizSubmitRequest;
import com.gsmv.quiz.mapper.QuizMapper;
import com.gsmv.quiz.model.QuizAnswer;
import com.gsmv.quiz.model.QuizQuestion;
import com.gsmv.quiz.model.QuizRecord;
import com.gsmv.security.SecurityUtils;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuizService {

    private final QuizMapper quizMapper;

    public QuizService(QuizMapper quizMapper) {
        this.quizMapper = quizMapper;
    }

    // ==================== Question CRUD ====================

    public PageResponse<QuizQuestion> listQuestions(String category, String type, String difficulty,
                                                     String keyword, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int offset = (safePage - 1) * safeSize;
        List<QuizQuestion> items = quizMapper.findPage(category, type, difficulty, keyword, safeSize, offset);
        long total = quizMapper.count(category, type, difficulty, keyword);
        return new PageResponse<>(items, total, safePage, safeSize);
    }

    public QuizQuestion getQuestion(Long id) {
        QuizQuestion q = quizMapper.findQuestionById(id);
        if (q == null) {
            throw new NotFoundException("题目不存在");
        }
        return q;
    }

    public QuizQuestion createQuestion(QuizQuestionSaveRequest request) {
        validateCategory(request.category());
        validateType(request.type());
        validateDifficulty(request.difficulty());
        QuizQuestion q = new QuizQuestion();
        q.setCategory(request.category());
        q.setType(request.type());
        q.setTitle(request.title());
        q.setOptions(request.options());
        q.setAnswer(request.answer());
        q.setExplanation(request.explanation());
        q.setDifficulty(request.difficulty());
        q.setStatus(1);
        quizMapper.insertQuestion(q);
        return quizMapper.findQuestionById(q.getId());
    }

    public QuizQuestion updateQuestion(Long id, QuizQuestionSaveRequest request) {
        QuizQuestion q = getQuestion(id);
        validateCategory(request.category());
        validateType(request.type());
        validateDifficulty(request.difficulty());
        q.setCategory(request.category());
        q.setType(request.type());
        q.setTitle(request.title());
        q.setOptions(request.options());
        q.setAnswer(request.answer());
        q.setExplanation(request.explanation());
        q.setDifficulty(request.difficulty());
        quizMapper.updateQuestion(q);
        return getQuestion(id);
    }

    public void deleteQuestion(Long id) {
        getQuestion(id);
        quizMapper.deleteQuestion(id);
    }

    public void toggleQuestionStatus(Long id) {
        QuizQuestion q = getQuestion(id);
        q.setStatus(q.getStatus() == 1 ? 0 : 1);
        quizMapper.updateQuestion(q);
    }

    public long countByCategory(String category) {
        return quizMapper.countByCategory(category);
    }

    // ==================== Exam ====================

    @Transactional
    public QuizExamStartResponse startExam(QuizExamRequest request) {
        List<String> categories = normalizeCategories(request.getCategories());
        int count = request.getCount() != null && request.getCount() > 0
                ? Math.min(request.getCount(), 200) : 20;
        String difficulty = normalizeNullable(request.getDifficulty());

        List<QuizQuestion> questions = quizMapper.findByFilters(
                categories, difficulty, count, "RANDOM".equalsIgnoreCase(request.getMode()));
        if (questions.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "当前筛选条件下没有可用题目", HttpStatus.NOT_FOUND);
        }

        QuizRecord record = new QuizRecord();
        record.setUserId(SecurityUtils.requireCurrentUser().userId());
        record.setScore(0);
        record.setTotal(questions.size());
        record.setCategories(categories.isEmpty() ? null : String.join(",", categories));
        record.setMode(request.getMode() != null ? request.getMode() : "RANDOM");
        quizMapper.insertRecord(record);

        return new QuizExamStartResponse(record.getId(), questions);
    }

    @Transactional
    public QuizResultResponse submitExam(QuizSubmitRequest request) {
        QuizRecord record = quizMapper.findRecordById(request.getRecordId());
        if (record == null) {
            throw new NotFoundException("答题记录不存在");
        }
        if (record.getFinishedAt() != null) {
            throw new BusinessException(ErrorCode.CONFLICT, "该试卷已提交，请勿重复提交", HttpStatus.CONFLICT);
        }

        int correctCount = 0;
        List<QuizResultResponse.QuestionResult> details = new ArrayList<>();

        for (QuizSubmitRequest.AnswerItem item : request.getAnswers()) {
            QuizQuestion question = quizMapper.findQuestionById(item.getQuestionId());
            if (question == null) continue;

            boolean isCorrect = isAnswerCorrect(question, item.getUserAnswer());
            if (isCorrect) correctCount++;

            QuizAnswer answer = new QuizAnswer();
            answer.setRecordId(record.getId());
            answer.setQuestionId(question.getId());
            answer.setUserAnswer(item.getUserAnswer());
            answer.setIsCorrect(isCorrect ? 1 : 0);
            quizMapper.insertAnswer(answer);

            QuizResultResponse.QuestionResult detail = new QuizResultResponse.QuestionResult();
            detail.setQuestionId(question.getId());
            detail.setTitle(question.getTitle());
            detail.setType(question.getType());
            detail.setOptions(question.getOptions());
            detail.setCorrectAnswer(question.getAnswer());
            detail.setUserAnswer(item.getUserAnswer());
            detail.setCorrect(isCorrect);
            detail.setExplanation(question.getExplanation());
            details.add(detail);
        }

        record.setScore(correctCount);
        record.setTotal(details.size());
        record.setFinishedAt(LocalDateTime.now());
        quizMapper.updateRecord(record);

        QuizResultResponse result = new QuizResultResponse();
        result.setRecordId(record.getId());
        result.setScore(correctCount);
        result.setTotal(details.size());
        result.setGrade(calcGrade(correctCount, details.size()));
        result.setDetails(details);
        return result;
    }

    public QuizResultResponse getExamResult(Long recordId) {
        QuizRecord record = quizMapper.findRecordById(recordId);
        if (record == null) {
            throw new NotFoundException("答题记录不存在");
        }
        List<QuizAnswer> answers = quizMapper.findAnswersByRecord(recordId);
        List<QuizResultResponse.QuestionResult> details = new ArrayList<>();
        for (QuizAnswer a : answers) {
            QuizQuestion q = quizMapper.findQuestionById(a.getQuestionId());
            if (q == null) continue;
            QuizResultResponse.QuestionResult d = new QuizResultResponse.QuestionResult();
            d.setQuestionId(q.getId());
            d.setTitle(q.getTitle());
            d.setType(q.getType());
            d.setOptions(q.getOptions());
            d.setCorrectAnswer(q.getAnswer());
            d.setUserAnswer(a.getUserAnswer());
            d.setCorrect(a.getIsCorrect() == 1);
            d.setExplanation(q.getExplanation());
            details.add(d);
        }
        QuizResultResponse result = new QuizResultResponse();
        result.setRecordId(record.getId());
        result.setScore(record.getScore());
        result.setTotal(record.getTotal());
        result.setGrade(calcGrade(record.getScore(), record.getTotal()));
        result.setDetails(details);
        return result;
    }

    public PageResponse<QuizRecord> listRecords(Long userId, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 50);
        int offset = (safePage - 1) * safeSize;
        Long uid = userId != null ? userId : SecurityUtils.requireCurrentUser().userId();
        List<QuizRecord> items = quizMapper.findRecordsByUser(uid, safeSize, offset);
        long total = quizMapper.countRecordsByUser(uid);
        return new PageResponse<>(items, total, safePage, safeSize);
    }

    // ==================== Private helpers ====================

    private boolean isAnswerCorrect(QuizQuestion question, String userAnswer) {
        if (userAnswer == null) return false;
        String correctAnswer = question.getAnswer().trim();
        String user = userAnswer.trim();
        if ("FILL".equals(question.getType())) {
            // 填空题：支持多个正确答案用 | 分隔；用户答案只需包含关键词即判对（忽略大小写和首尾空格）
            String[] acceptedAnswers = correctAnswer.split("\\|");
            String userLower = user.toLowerCase();
            for (String ans : acceptedAnswers) {
                String ansTrimmed = ans.trim().toLowerCase();
                if (!ansTrimmed.isEmpty() && userLower.contains(ansTrimmed)) {
                    return true;
                }
            }
            return false;
        }
        return user.equalsIgnoreCase(correctAnswer);
    }

    private String calcGrade(int score, int total) {
        if (total == 0) return "N/A";
        double pct = 100.0 * score / total;
        if (pct >= 90) return "优秀";
        if (pct >= 75) return "良好";
        if (pct >= 60) return "及格";
        return "需加强";
    }

    private List<String> normalizeCategories(List<String> categories) {
        if (categories == null) return List.of();
        List<String> valid = new ArrayList<>();
        for (String c : categories) {
            if (c != null && !c.isBlank()) {
                String trimmed = c.trim().toUpperCase();
                try { validateCategory(trimmed); valid.add(trimmed); }
                catch (BusinessException ignored) {}
            }
        }
        return valid;
    }

    private void validateCategory(String c) {
        if (!List.of("SHIP", "WEATHER", "SEA_AREA").contains(c)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "分类必须是 SHIP / WEATHER / SEA_AREA 之一", HttpStatus.BAD_REQUEST);
        }
    }

    private void validateType(String t) {
        if (!List.of("SINGLE", "MULTI", "JUDGE", "FILL").contains(t)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "题型必须是 SINGLE / MULTI / JUDGE / FILL 之一", HttpStatus.BAD_REQUEST);
        }
    }

    private void validateDifficulty(String d) {
        if (!List.of("EASY", "MEDIUM", "HARD").contains(d)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "难度必须是 EASY / MEDIUM / HARD 之一", HttpStatus.BAD_REQUEST);
        }
    }

    private String normalizeNullable(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
