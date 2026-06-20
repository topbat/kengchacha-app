package com.kengchacha.toolbox;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 风险检测记录（私密）。出于隐私只落库脱敏预览与结论，不存原文（技术方案 §5.5：加密/最小化）。
 */
@Getter
@Setter
@Entity
@Table(name = "tool_detection")
public class DetectionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer toolType;     // 1合同体检 2链接验毒 3拍照识坑 4收款核验
    private String preview;       // 输入脱敏预览（截断 + 打码）
    private Integer riskLevel;    // 1低 2中 3高
    private Integer score;        // 0-100 风险分
    private LocalDateTime createdAt;
}
