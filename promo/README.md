# 坑查查 · 产品发布宣传片（可播放/可录制）

一支与 [`docs/宣传片脚本-Storyboard.md`](../docs/宣传片脚本-Storyboard.md) 同步的 **自动播放动态宣传片**：纯 HTML/CSS/JS，无外部依赖，用项目真机截图 + 品牌动效呈现，时长 **约 5 分钟**。

## 播放
直接用浏览器（Chrome/Edge 最佳）打开本目录的 `index.html` 即可自动播放：
- 打开后建议 **F11 全屏**；舞台固定 1920×1080，会自适应缩放，构图始终一致。
- 快捷键：`空格` 暂停/播放 · `H` 隐藏/显示界面控件 · `R` 重新播放。

> 截图通过相对路径引用 `../docs/screenshots/*`，请保持目录结构（在仓库内打开即可）。

## 导出为视频（MP4/WebM）
本片为「动态排版 + 真机截图」式样，**默认静音**（无内嵌配音/配乐）。导出真实视频有两种方式：

1. **录屏**（最简单）：全屏播放，用 OBS / 系统录屏录制一遍，得到 1080p 视频。
2. **无头录制**（命令行，输出 WebM）：
   ```bash
   npm i -g agent-browser && agent-browser install
   agent-browser set viewport 1920 1080
   agent-browser record start 坑查查-宣传片.webm "file:///<仓库绝对路径>/promo/index.html"
   # 播放约 5 分钟后：
   agent-browser record stop
   # 如需 mp4：ffmpeg -i 坑查查-宣传片.webm -c:v libx264 -pix_fmt yuv420p 坑查查-宣传片.mp4
   ```

## 配音 / 配乐（可选，进一步提升质感）
按脚本《旁白全文》录制配音（约 4'50"），再叠加轻电子 + 钢琴 BGM：
- 字幕已硬编码在画面底部，可直接对口型录音；
- 高潮点对齐 `03:54`（横切能力）与 `04:44`（收尾）。

---
出品：**topbat** · 《有坑没坑，先查查》
