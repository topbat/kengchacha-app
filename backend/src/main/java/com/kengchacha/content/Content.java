package com.kengchacha.content;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 避坑内容（四要素）。 */
@Getter
@Setter
@Entity
@Table(name = "content")
public class Content {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String victimGroup;   // 踩坑人群
    private String trick;         // 坑人套路
    private String loss;          // 损失后果
    private String tip;           // 避坑常识
    private String slogan;        // 一句话口诀
    private String body;          // 正文

    private Integer sourceType;   // 1真实经历 2官方预警 3判例投诉
    private String sourceRef;
    private Double credibility;
    private Integer hazardLevel;  // 1低 2中 3高
    private Integer lossAmount;   // 1<1k 2~1w 3~10w 4>10w
    private Integer hotScore;
    private LocalDateTime onlineAt;
}
