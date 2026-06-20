package com.kengchacha.media;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 内容素材（海报/GIF/信息长图/音频）。本 MVP 落库海报生成记录（media_type=1）。 */
@Getter
@Setter
@Entity
@Table(name = "media_asset")
public class MediaAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long contentId;       // 关联避坑内容（可空：自定义海报）
    private Integer mediaType;    // 1海报 2GIF 3信息长图 4音频(TTS)
    private String title;
    private String theme;
    private LocalDateTime createdAt;
}
