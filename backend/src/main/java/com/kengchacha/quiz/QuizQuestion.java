package com.kengchacha.quiz;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "quiz_question")
public class QuizQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String stem;
    private String dimension;   // 心理/法律/消费/金融/职场/网络
    private Double difficulty;
}
