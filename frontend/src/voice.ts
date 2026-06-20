// 语音能力（前端）：在线优先用浏览器 Web Speech API；不支持时回退后端 /api/voice。
// ASR（语音转文字）：webkitSpeechRecognition；TTS（朗读）：speechSynthesis。
import { apiPost } from './api'

export function asrSupported(): boolean {
  return typeof window !== 'undefined' &&
    !!((window as any).SpeechRecognition || (window as any).webkitSpeechRecognition)
}

export function ttsSupported(): boolean {
  return typeof window !== 'undefined' && 'speechSynthesis' in window
}

/** 一次性语音识别，返回最终文本；interim 回调可实时显示。 */
export function listenOnce(opts?: { lang?: string; interim?: (t: string) => void }): Promise<string> {
  return new Promise((resolve, reject) => {
    const Ctor = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition
    if (!Ctor) { reject(new Error('当前浏览器不支持语音输入，请改用键盘')); return }
    const rec: any = new Ctor()
    rec.lang = opts?.lang || 'zh-CN'
    rec.interimResults = !!opts?.interim
    rec.maxAlternatives = 1
    let finalText = ''
    rec.onresult = (e: any) => {
      let interim = ''
      for (let i = e.resultIndex; i < e.results.length; i++) {
        const r = e.results[i]
        if (r.isFinal) finalText += r[0].transcript
        else interim += r[0].transcript
      }
      opts?.interim?.((finalText + interim).trim())
    }
    rec.onerror = (e: any) => reject(new Error(e.error === 'not-allowed' ? '麦克风权限被拒绝' : '语音识别失败'))
    rec.onend = () => resolve(finalText.trim())
    try { rec.start() } catch (e) { reject(e as Error) }
  })
}

/** 朗读文本（适老化播报）。在线用 speechSynthesis；否则播放后端合成提示音。 */
export async function speak(text: string, opts?: { rate?: number; lang?: string }): Promise<void> {
  if (!text) return
  if (ttsSupported()) {
    window.speechSynthesis.cancel()
    const u = new SpeechSynthesisUtterance(text)
    u.lang = opts?.lang || 'zh-CN'
    u.rate = opts?.rate ?? 1
    window.speechSynthesis.speak(u)
    return
  }
  try {
    const r: any = await apiPost('/voice/tts', { text })
    const audio = new Audio('data:' + r.mime + ';base64,' + r.audioBase64)
    await audio.play()
  } catch { /* 静默失败 */ }
}

export function stopSpeak() {
  if (ttsSupported()) window.speechSynthesis.cancel()
}
