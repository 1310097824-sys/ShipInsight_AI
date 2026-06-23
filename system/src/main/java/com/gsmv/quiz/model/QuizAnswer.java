package com.gsmv.quiz.model;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class QuizAnswer {

    private Long id;
    private Long recordId;
    private Long questionId;
    private String userAnswer;
    private Integer isCorrect;
    private LocalDateTime createdAt;
}
