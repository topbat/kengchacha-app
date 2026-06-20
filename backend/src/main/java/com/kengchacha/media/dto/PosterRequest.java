package com.kengchacha.media.dto;

/**
 * 海报生成入参。
 * 提供 contentId 则从避坑内容取数；也可直接传四要素自定义。
 * theme：auto（按危害等级配色）/ dark / warm。
 */
public record PosterRequest(Long contentId, String title, String trick, String loss,
                            String tip, String slogan, Integer hazardLevel, String theme) {
}
