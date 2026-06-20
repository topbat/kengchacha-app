package com.kengchacha.quiz;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 测试记录：多维得分/画像/建议以 JSON 文本存储。 */
@Getter
@Setter
@Entity
@Table(name = "test_record")
public class TestRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer scale;
    private Integer mode;
    private Integer score;

    @Column(columnDefinition = "TEXT")
    private String dimScores;
    @Column(columnDefinition = "TEXT")
    private String profileTags;
    @Column(columnDefinition = "TEXT")
    private String riskScenes;
    @Column(columnDefinition = "TEXT")
    private String advice;

    private LocalDateTime createdAt;
}
