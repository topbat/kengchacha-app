package com.kengchacha.guardian;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 守护预警：按家人订阅领域推送的高危提醒（一条提醒对应一条避坑内容）。 */
@Getter
@Setter
@Entity
@Table(name = "guardian_alert")
public class GuardianAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long relationId;      // 关联守护关系
    private Long contentId;       // 关联避坑内容
    private String memberName;    // 冗余：被守护家人（便于列表展示）
    private String topic;         // 命中的风险领域
    private Integer level;        // 1低 2中 3高
    private String title;         // 预警标题
    private String body;          // 一句话提醒（适老化口播稿）
    private Boolean readFlag;     // 是否已读
    private LocalDateTime createdAt;
}
