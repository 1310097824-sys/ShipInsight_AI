package com.gsmv.quiz.model;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class QuizRecord {

    private Long id;
    private Long userId;
    private Integer score;
    private Integer total;
    private String categories;
    private String mode;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}
