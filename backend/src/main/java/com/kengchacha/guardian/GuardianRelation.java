package com.kengchacha.guardian;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 家人守护关系：我为家人订阅风险领域，命中新高危预警时推送提醒。 */
@Getter
@Setter
@Entity
@Table(name = "guardian_relation")
public class GuardianRelation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String ownerName;     // 守护人（我）
    private String memberName;    // 被守护家人
    private String relation;      // 父母/子女/配偶/祖辈/其他
    private String phoneMask;     // 家人手机号（仅存掩码，隐私最小化）
    private String topics;        // 订阅的风险领域（逗号分隔，对应内容领域标签）
    private Boolean voiceFirst;   // 适老化：语音优先（大字+播报）
    private LocalDateTime createdAt;
}
