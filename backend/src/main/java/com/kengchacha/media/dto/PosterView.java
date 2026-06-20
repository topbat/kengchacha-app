package com.kengchacha.media.dto;

/** 海报渲染结果：svg 为完整可内联渲染的 SVG 字符串，前端可下载/转 PNG。 */
public record PosterView(Long assetId, String title, int width, int height, String theme, String svg) {
}
