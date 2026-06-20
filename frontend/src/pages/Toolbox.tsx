import { useState } from 'react'
import { apiGet, apiPost } from '../api'
import { asrSupported, listenOnce } from '../voice'

type Hit = { label: string; snippet: string; detail: string; advice: string; severity: number }
type Result = {
  toolType: number; riskLevel: number; score: number; verdict: string
  summary: string; hits: Hit[]; advice: string[]; disclaimer: string
}
type Rec = { id: number; toolType: number; toolName: string; preview: string; riskLevel: number; score: number; createdAt: string }

const TOOLS = [
  { type: 1, key: 'contract', ic: '📄', name: '合同体检', label: '合同 / 条款正文',
    ph: '粘贴合同或条款文本，自动标红「定金不退、自动续费、空白条款、培训贷、高额违约金」等风险点…',
    demo: '本协议定金一经收取概不退还；最终解释权归本公司所有；到期自动续费；如需上岗须先参加培训并办理分期贷款。' },
  { type: 2, key: 'link', ic: '🔗', name: '链接验毒', label: '可疑链接 URL',
    ph: 'http(s):// 粘贴可疑链接，检测仿冒域名 / IP 直连 / 诱导关键词…',
    demo: 'http://taob0a-anquan.verify-login.cn/unfreeze?安全账户' },
  { type: 3, key: 'image', ic: '📷', name: '拍照识坑', label: '截图 / 对话文字（OCR 结果）',
    ph: '把截图里的文字粘贴进来（或点🎤口述），识别「先付款、走私户、屏幕共享、征信修复」等诈骗话术…',
    demo: '我是公安局的，你涉嫌洗钱，请把钱转到安全账户自证清白，并开启屏幕共享配合调查。' },
  { type: 4, key: 'payee', ic: '💳', name: '收款核验', label: '收款账号 / 二维码信息',
    ph: '收款账号或二维码信息…',
    demo: '微信号 wxpay8866（个人）', hint: true },
]

const SEV = (s: number) => s === 3 ? { cls: 'r', txt: '高危' } : s === 2 ? { cls: 'a', txt: '中危' } : { cls: 'g', txt: '低危' }

export default function Toolbox() {
  const [active, setActive] = useState(TOOLS[0])
  const [input, setInput] = useState('')
  const [hint, setHint] = useState('')
  const [result, setResult] = useState<Result | null>(null)
  const [loading, setLoading] = useState(false)
  const [listening, setListening] = useState(false)
  const [recs, setRecs] = useState<Rec[] | null>(null)
  const [msg, setMsg] = useState('')

  function switchTool(t: typeof TOOLS[number]) {
    setActive(t); setInput(''); setHint(''); setResult(null); setMsg('')
  }

  async function detect() {
    if (!input.trim()) { setMsg('请输入待检测内容'); return }
    setLoading(true); setResult(null); setMsg('')
    try {
      const r: Result = await apiPost('/toolbox/' + active.key, { input, hint })
      setResult(r)
    } catch (e: any) { setMsg('检测失败：' + e.message) }
    finally { setLoading(false) }
  }

  async function dictate() {
    if (!asrSupported()) { setMsg('当前浏览器不支持语音输入，请改用键盘'); return }
    setListening(true); setMsg('🎤 正在聆听，请口述截图内容…')
    try {
      const text = await listenOnce({ interim: (t) => setInput(t) })
      if (text) setInput(text)
      setMsg('')
    } catch (e: any) { setMsg(e.message) }
    finally { setListening(false) }
  }

  async function loadRecords() {
    if (recs) { setRecs(null); return }
    try { const d = await apiGet('/toolbox/records?size=20'); setRecs(d.items) }
    catch (e: any) { setMsg('记录加载失败：' + e.message) }
  }

  return (
    <div>
      <div className="appbar">
        <div className="brand">🧰 风险检测工具箱</div>
        <div className="sub">合同体检 · 链接验毒 · 拍照识坑 · 收款核验</div>
      </div>

      <div className="toolgrid">
        {TOOLS.map((t) => (
          <div key={t.key} className={'tool' + (active.key === t.key ? ' on' : '')} onClick={() => switchTool(t)}>
            <div className="ic">{t.ic}</div><span>{t.name}</span>
          </div>
        ))}
      </div>

      <div className="form">
        <div className="lbl">{active.label}
          {active.key === 'image' && asrSupported() &&
            <span className="mini-mic" onClick={dictate}>{listening ? '● 聆听中' : '🎤 语音输入'}</span>}
        </div>
        <textarea className="textarea" value={input} placeholder={active.ph} onChange={(e) => setInput(e.target.value)} />
        {active.hint && (
          <>
            <div className="lbl">对方自称（公司/平台/客服名，可选）</div>
            <input className="input" value={hint} placeholder="如：某某官方旗舰店客服" onChange={(e) => setHint(e.target.value)} />
          </>
        )}
        <div style={{ display: 'flex', gap: 8, marginTop: 6 }}>
          <button className="btn ghost" style={{ flex: '0 0 auto', width: 110 }} onClick={() => setInput(active.demo)}>填入示例</button>
          <button className="btn" onClick={detect} disabled={loading}>{loading ? '检测中…' : '开始检测'}</button>
        </div>
        {msg && <div style={{ color: '#0B6E6B', fontSize: 13, margin: '10px 2px' }}>{msg}</div>}
      </div>

      {result && (
        <div className="list">
          <div className={'verdict ' + SEV(result.riskLevel).cls}>
            <div className="vtop">
              <span className="vbig">{result.verdict}</span>
              <span className="vscore">风险分 {result.score}</span>
            </div>
            <div className="track"><div className="fill" style={{ width: result.score + '%' }} /></div>
            <div className="vsum">{result.summary}</div>
          </div>

          {result.hits.map((h, i) => {
            const s = SEV(h.severity)
            return (
              <div className="card" key={i}>
                <div className="top">
                  <span className={'haz ' + s.cls}><span className="dot" />{s.txt}</span>
                  <b style={{ fontSize: 13.5 }}>{h.label}</b>
                </div>
                {h.snippet && <div className="hit-snip">命中：{h.snippet}</div>}
                <div style={{ fontSize: 12.5, color: '#475569', margin: '5px 0' }}>{h.detail}</div>
                <div className="slogan">✅ {h.advice}</div>
              </div>
            )
          })}

          <div className="card">
            <b style={{ fontSize: 13 }}>处置建议</b>
            <ol style={{ margin: '6px 0 0 18px', fontSize: 13, color: '#334155' }}>
              {result.advice.map((a, i) => <li key={i} style={{ marginBottom: 4 }}>{a}</li>)}
            </ol>
            <div className="disc" style={{ marginTop: 8 }}>{result.disclaimer}</div>
          </div>
        </div>
      )}

      <div className="list" style={{ paddingTop: 0 }}>
        <button className="btn ghost" onClick={loadRecords}>{recs ? '收起检测记录' : '🧾 检测记录（私密）'}</button>
        {recs && recs.length === 0 && <div className="empty">暂无检测记录</div>}
        {recs && recs.map((r) => {
          const s = SEV(r.riskLevel)
          return (
            <div className="card" key={r.id} style={{ marginTop: 10 }}>
              <div className="top">
                <span className={'haz ' + s.cls}><span className="dot" />{s.txt}</span>
                <span className="src">{r.toolName} · 分 {r.score}</span>
              </div>
              <div style={{ fontSize: 12.5, color: '#475569' }}>{r.preview}</div>
            </div>
          )
        })}
        {recs && <div className="disc" style={{ padding: '4px 4px 0' }}>记录仅保存脱敏预览，不存储原文（隐私最小化）。</div>}
      </div>
    </div>
  )
}
