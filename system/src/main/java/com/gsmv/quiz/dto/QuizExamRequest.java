package com.gsmv.quiz.dto;

import java.util.List;

public class QuizExamRequest {

    private List<String> categories;
    private Integer count;
    private String mode;   // RANDOM / SEQUENTIAL
    private String difficulty; // EASY / MEDIUM / HARD / null=all

    public List<String> getCategories() { return categories; }
    public void setCategories(List<String> categories) { this.categories = categories; }

    public Integer getCount() { return count; }
    public void setCount(Integer count) { this.count = count; }

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
}
