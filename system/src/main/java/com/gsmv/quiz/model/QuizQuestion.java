package com.gsmv.quiz.model;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class QuizQuestion {

    private Long id;
    private String category;
    private String type;
    private String title;
    private String options; // JSON string
    private String answer;
    private String explanation;
    private String difficulty;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
