import { useEffect, useState } from 'react'
import { apiGet, apiPost } from '../api'
import { asrSupported, listenOnce } from '../voice'

type Story = {
  id: number; nickname: string; happenTime: string; region: string
  groupTag: string; domainTag: string; content: string; advice: string
  likeLearn: number; likePity: number
}

const empty = { nickname: '', region: '', groupTag: '', domainTag: '', content: '', advice: '' }

export default function Report() {
  const [form, setForm] = useState({ ...empty })
  const [stories, setStories] = useState<Story[]>([])
  const [msg, setMsg] = useState('')
  const [listening, setListening] = useState(false)

  function load() { apiGet('/ugc/stories?size=20').then((d) => setStories(d.items)) }
  useEffect(() => { load() }, [])

  function set(k: string, v: string) { setForm((f) => ({ ...f, [k]: v })) }

  // 语音转写：浏览器在线识别，结果实时填入事件经过
  async function dictate() {
    if (!asrSupported()) { aiDraft(); setMsg('当前浏览器不支持语音输入，已用模板生成草稿'); return }
    setListening(true); setMsg('🎤 正在聆听，说出你的踩坑经过…')
    try {
      const base = form.content ? form.content + ' ' : ''
      const text = await listenOnce({ interim: (t) => set('content', base + t) })
      set('content', (base + text).trim())
      setMsg('语音转写完成，可点“AI 成稿”再润色')
    } catch (e: any) { setMsg(e.message) }
    finally { setListening(false) }
  }

  // 模拟“语音转写 + AI 成稿”：把口述要点套成结构化草稿
  function aiDraft() {
    set('content', form.content || '应聘一份兼职，对方以各种理由要求我先付款（押金/保证金/会员费），付款后就联系不上了。')
    if (!form.advice) set('advice', '正规招聘绝不收押金、培训费；先收钱的一律拒绝。')
    if (!form.groupTag) set('groupTag', '求职大学生')
    if (!form.domainTag) set('domainTag', '求职就业')
    setMsg('AI 已根据要点生成草稿，可继续修改')
  }

  async function submit() {
    try {
      await apiPost('/ugc/stories', form)
      setMsg('提交成功，已进入审核（AI + 人工双审）')
      setForm({ ...empty }); load()
    } catch (e: any) { setMsg('提交失败：' + e.message) }
  }
  async function like(id: number, type: string) {
    await apiPost(`/ugc/stories/${id}/like?type=${type}`)
    load()
  }

  return (
    <div>
      <div className="appbar plain"><div className="brand">✍️ 上报我踩过的坑</div><div className="sub">帮别人少踩坑 · 可领防坑贡献值</div></div>

      <div className="form">
        <div className="aidraft">
          <span className="tagai">语音 + AI 成稿</span>
          <div>不会打字？点“🎤 语音转写”说出经过（浏览器在线识别），再点“AI 成稿”自动整理结构。</div>
          <div style={{ display: 'flex', gap: 8, marginTop: 8 }}>
            <button className="btn ghost" onClick={dictate} disabled={listening}>{listening ? '● 聆听中…' : '🎤 语音转写'}</button>
            <button className="btn" onClick={aiDraft}>✍️ AI 成稿</button>
          </div>
        </div>

        <div className="lbl">昵称</div>
        <input className="input" value={form.nickname} onChange={(e) => set('nickname', e.target.value)} placeholder="可不填，默认“热心网友”" />
        <div className="lbl">地区 / 人群 / 领域</div>
        <div style={{ display: 'flex', gap: 8 }}>
          <input className="input" value={form.region} onChange={(e) => set('region', e.target.value)} placeholder="地区" />
          <input className="input" value={form.groupTag} onChange={(e) => set('groupTag', e.target.value)} placeholder="人群" />
          <input className="input" value={form.domainTag} onChange={(e) => set('domainTag', e.target.value)} placeholder="领域" />
        </div>
        <div className="lbl">事件经过 *</div>
        <textarea className="textarea" value={form.content} onChange={(e) => set('content', e.target.value)} placeholder="说说你是怎么踩坑的…（至少 5 字）" />
        <div className="lbl">避坑建议</div>
        <input className="input" value={form.advice} onChange={(e) => set('advice', e.target.value)} placeholder="一句话提醒后来人" />

        {msg && <div style={{ color: '#0B6E6B', fontSize: 13, margin: '10px 2px' }}>{msg}</div>}
        <div style={{ marginTop: 12 }}><button className="btn" onClick={submit}>提交审核</button></div>
      </div>

      <div className="appbar plain" style={{ marginTop: 6 }}><div className="brand">踩坑广场</div><div className="sub">真实经历 · 审核通过展示</div></div>
      <div className="list">
        {stories.map((s) => (
          <div className="card" key={s.id}>
            <div className="top">
              <b style={{ fontSize: 13 }}>{s.nickname}</b>
              <span className="src">{[s.region, s.groupTag, s.domainTag].filter(Boolean).join(' · ')}</span>
            </div>
            <div style={{ fontSize: 13, color: '#334155', margin: '4px 0' }}>{s.content}</div>
            {s.advice && <div className="slogan">💡 {s.advice}</div>}
            <div className="meta">
              <span style={{ cursor: 'pointer' }} onClick={() => like(s.id, 'learn')}>👍 学到了 {s.likeLearn}</span>
              <span style={{ cursor: 'pointer' }} onClick={() => like(s.id, 'pity')}>🫶 点亮 {s.likePity}</span>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
