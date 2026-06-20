package com.kengchacha.ugc;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "ugc_story")
public class UgcStory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nickname;
    private String happenTime;
    private String region;
    private String groupTag;
    private String domainTag;
    private String content;
    private String advice;

    private Integer likeLearn;
    private Integer likePity;
    private Integer auditStatus;   // 0待审 1AI通过 2人工通过 3驳回
    private LocalDateTime createdAt;
}
