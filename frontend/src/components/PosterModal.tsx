import { useEffect, useState } from 'react'
import { apiPost } from '../api'

type PosterView = { assetId: number; title: string; width: number; height: number; theme: string; svg: string }
const THEMES = [['auto', '自动'], ['dark', '暗色'], ['warm', '亮色']]

/** 文生图海报弹窗：调用 /api/share/poster 取服务端 SVG，可切主题、下载 SVG/PNG。 */
export default function PosterModal({ contentId, onClose }: { contentId: number; onClose: () => void }) {
  const [theme, setTheme] = useState('auto')
  const [view, setView] = useState<PosterView | null>(null)
  const [err, setErr] = useState('')

  useEffect(() => {
    let alive = true
    setView(null); setErr('')
    apiPost('/share/poster', { contentId, theme })
      .then((d: PosterView) => { if (alive) setView(d) })
      .catch((e) => alive && setErr(e.message))
    return () => { alive = false }
  }, [contentId, theme])

  function save(blob: Blob, name: string) {
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url; a.download = name; a.click()
    setTimeout(() => URL.revokeObjectURL(url), 1500)
  }
  function downloadSvg() {
    if (!view) return
    save(new Blob([view.svg], { type: 'image/svg+xml;charset=utf-8' }), `坑查查海报-${contentId}.svg`)
  }
  function downloadPng() {
    if (!view) return
    const blob = new Blob([view.svg], { type: 'image/svg+xml;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const img = new Image()
    img.onload = () => {
      const canvas = document.createElement('canvas')
      canvas.width = view.width; canvas.height = view.height
      const ctx = canvas.getContext('2d')
      if (ctx) {
        ctx.drawImage(img, 0, 0)
        canvas.toBlob((b) => { if (b) save(b, `坑查查海报-${contentId}.png`) }, 'image/png')
      }
      URL.revokeObjectURL(url)
    }
    img.onerror = () => { URL.revokeObjectURL(url); downloadSvg() }
    img.src = url
  }

  return (
    <div className="overlay" onClick={onClose}>
      <div className="sheet" onClick={(e) => e.stopPropagation()}>
        <div className="sheet-head">
          <b>🖼 生成分享海报</b>
          <span className="x" onClick={onClose}>✕</span>
        </div>
        <div className="theme-row">
          {THEMES.map(([v, label]) => (
            <span key={v} className={'chip' + (theme === v ? ' on' : '')} onClick={() => setTheme(v)}>{label}</span>
          ))}
        </div>
        <div className="poster-wrap">
          {err && <div className="err">{err}</div>}
          {!view && !err && <div className="loading">海报生成中…</div>}
          {view && <div className="poster-svg" dangerouslySetInnerHTML={{ __html: view.svg }} />}
        </div>
        <div className="btn-row" style={{ padding: '10px 0 0' }}>
          <button className="btn ghost" onClick={downloadSvg} disabled={!view}>下载 SVG</button>
          <button className="btn" onClick={downloadPng} disabled={!view}>保存图片</button>
        </div>
      </div>
    </div>
  )
}
