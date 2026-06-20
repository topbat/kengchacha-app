package com.kengchacha.content;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** 五维标签：dimension 1阶段 2领域 3心理 4渠道 5损失类型。 */
@Getter
@Setter
@Entity
@Table(name = "content_tag")
public class ContentTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long contentId;
    private Integer dimension;
    private String tag;
}
