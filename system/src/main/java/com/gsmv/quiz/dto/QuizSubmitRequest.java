package com.gsmv.quiz.dto;

import java.util.List;
import java.util.Map;

public class QuizSubmitRequest {

    private Long recordId;
    private List<AnswerItem> answers;

    public Long getRecordId() { return recordId; }
    public void setRecordId(Long recordId) { this.recordId = recordId; }

    public List<AnswerItem> getAnswers() { return answers; }
    public void setAnswers(List<AnswerItem> answers) { this.answers = answers; }

    public static class AnswerItem {
        private Long questionId;
        private String userAnswer;

        public Long getQuestionId() { return questionId; }
        public void setQuestionId(Long questionId) { this.questionId = questionId; }

        public String getUserAnswer() { return userAnswer; }
        public void setUserAnswer(String userAnswer) { this.userAnswer = userAnswer; }
    }
}
