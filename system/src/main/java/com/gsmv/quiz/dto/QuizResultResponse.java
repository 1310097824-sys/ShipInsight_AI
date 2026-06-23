package com.gsmv.quiz.dto;

import java.util.List;
import java.util.Map;

public class QuizResultResponse {

    private Long recordId;
    private int score;
    private int total;
    private String grade;
    private List<QuestionResult> details;

    public Long getRecordId() { return recordId; }
    public void setRecordId(Long recordId) { this.recordId = recordId; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }

    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }

    public List<QuestionResult> getDetails() { return details; }
    public void setDetails(List<QuestionResult> details) { this.details = details; }

    public static class QuestionResult {
        private Long questionId;
        private String title;
        private String type;
        private String options;
        private String correctAnswer;
        private String userAnswer;
        private boolean isCorrect;
        private String explanation;

        public Long getQuestionId() { return questionId; }
        public void setQuestionId(Long questionId) { this.questionId = questionId; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getOptions() { return options; }
        public void setOptions(String options) { this.options = options; }

        public String getCorrectAnswer() { return correctAnswer; }
        public void setCorrectAnswer(String correctAnswer) { this.correctAnswer = correctAnswer; }

        public String getUserAnswer() { return userAnswer; }
        public void setUserAnswer(String userAnswer) { this.userAnswer = userAnswer; }

        public boolean isCorrect() { return isCorrect; }
        public void setCorrect(boolean correct) { isCorrect = correct; }

        public String getExplanation() { return explanation; }
        public void setExplanation(String explanation) { this.explanation = explanation; }
    }
}
