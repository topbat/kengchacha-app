import { useEffect, useState } from 'react'
import { apiGet, apiPost } from '../api'
import { speak } from '../voice'

type Relation = {
  id: number; ownerName: string; memberName: string; relation: string
  phoneMask: string; topics: string[]; voiceFirst: boolean; alertCount: number
}
type Alert = {
  id: number; relationId: number; contentId: number; memberName: string; topic: string
  level: number; title: string; body: string; readFlag: boolean; createdAt: string
}
type Overview = { relationCount: number; unreadCount: number; relations: Relation[]; alerts: Alert[] }

const RELATIONS = ['父母', '子女', '配偶', '祖辈', '其他']
const empty = { memberName: '', relation: '父母', phone: '', voiceFirst: true }

export default function Guardian() {
  const [ov, setOv] = useState<Overview | null>(null)
  const [topicOpts, setTopicOpts] = useState<string[]>([])
  const [form, setForm] = useState({ ...empty })
  const [topics, setTopics] = useState<string[]>([])
  const [showForm, setShowForm] = useState(false)
  const [big, setBig] = useState(false)
  const [msg, setMsg] = useState('')

  function load() { apiGet('/guardian/overview').then(setOv).catch((e) => setMsg(e.message)) }
  useEffect(() => {
    load()
    apiGet('/content/tags').then((gs: any[]) => {
      const dom = gs.find((g) => g.dimension === 2)
      setTopicOpts(dom ? dom.tags : [])
    })
  }, [])

  function toggleTopic(t: string) {
    setTopics((ts) => ts.includes(t) ? ts.filter((x) => x !== t) : [...ts, t])
  }

  async function bind() {
    if (!form.memberName.trim()) { setMsg('请填写家人称呼'); return }
    try {
      await apiPost('/guardian/relations', { ...form, topics })
      setForm({ ...empty }); setTopics([]); setShowForm(false); setMsg('已添加守护关系')
      load()
    } catch (e: any) { setMsg('添加失败：' + e.message) }
  }
  async function unbind(id: number) {
    await fetch(`/api/guardian/relations/${id}`, { method: 'DELETE' })
    load()
  }
  async function push(id: number) {
    const created: Alert[] = await apiPost(`/guardian/relations/${id}/push`)
    setMsg(created.length ? `已推送 ${created.length} 条新预警` : '暂无新的高危预警可推送')
    load()
  }
  async function pushAll() {
    const created: Alert[] = await apiPost('/guardian/push-all')
    setMsg(created.length ? `已为家人推送 ${created.length} 条新预警` : '暂无新的高危预警可推送')
    load()
  }
  async function markRead(id: number) {
    await apiPost(`/guardian/alerts/${id}/read`); load()
  }

  if (!ov) return <div className="loading">加载中…</div>
  const lv = (n: number) => n === 3 ? 'r' : n === 2 ? 'a' : 'g'

  return (
    <div className={big ? 'elder' : ''}>
      <div className="appbar">
        <div className="brand">👵 家人守护</div>
        <div className="sub">替家人订阅风险领域 · 一键扫描推送高危预警</div>
      </div>

      <div className="guard-bar">
        <div><b style={{ fontSize: 20 }}>{ov.relationCount}</b><span>守护家人</span></div>
        <div><b style={{ fontSize: 20, color: '#EF4444' }}>{ov.unreadCount}</b><span>未读预警</span></div>
        <button className="chip on" onClick={() => setBig((b) => !b)}>{big ? '标准字号' : '🔍 大字模式'}</button>
      </div>

      <div className="btn-row">
        <button className="btn ghost" onClick={() => setShowForm((s) => !s)}>{showForm ? '取消' : '＋ 添加家人'}</button>
        <button className="btn" onClick={pushAll}>🛡️ 一键守护扫描</button>
      </div>

      {showForm && (
        <div className="form" style={{ paddingTop: 0 }}>
          <div className="lbl">家人称呼 *</div>
          <input className="input" value={form.memberName} placeholder="如：妈妈 / 张阿姨"
                 onChange={(e) => setForm({ ...form, memberName: e.target.value })} />
          <div className="lbl">关系</div>
          <div className="chiprow">
            {RELATIONS.map((r) => (
              <span key={r} className={'chip' + (form.relation === r ? ' on' : '')} onClick={() => setForm({ ...form, relation: r })}>{r}</span>
            ))}
          </div>
          <div className="lbl">家人手机号（仅存掩码，可选）</div>
          <input className="input" value={form.phone} placeholder="13800006677"
                 onChange={(e) => setForm({ ...form, phone: e.target.value })} />
          <div className="lbl">订阅风险领域（命中新高危内容时提醒）</div>
          <div className="chiprow" style={{ flexWrap: 'wrap' }}>
            {topicOpts.map((t) => (
              <span key={t} className={'chip' + (topics.includes(t) ? ' on' : '')} onClick={() => toggleTopic(t)}>{t}</span>
            ))}
          </div>
          <label className="lbl" style={{ cursor: 'pointer' }}>
            <input type="checkbox" checked={form.voiceFirst} onChange={(e) => setForm({ ...form, voiceFirst: e.target.checked })} />
            适老化：语音优先（大字 + 朗读播报）
          </label>
          <button className="btn" style={{ marginTop: 10 }} onClick={bind}>保存守护关系</button>
        </div>
      )}

      {msg && <div style={{ color: '#0B6E6B', fontSize: 13, margin: '6px 14px' }}>{msg}</div>}

      <div className="appbar plain"><div className="brand" style={{ fontSize: 15 }}>守护家人</div></div>
      <div className="list">
        {ov.relations.length === 0 && <div className="empty">还没有守护的家人，点上方“添加家人”。</div>}
        {ov.relations.map((r) => (
          <div className="card" key={r.id}>
            <div className="top">
              <b style={{ fontSize: 14 }}>{r.memberName}</b>
              <span className="tg">{r.relation}</span>
              {r.voiceFirst && <span className="tg" style={{ background: '#F0FBF8', color: '#0B6E6B' }}>🔊 适老</span>}
              <span className="src">{r.phoneMask || '未填手机号'}</span>
            </div>
            <div className="tags">
              {r.topics.length ? r.topics.map((t) => <span className="tg" key={t}>#{t}</span>)
                : <span className="tg">未订阅领域（默认全部高危）</span>}
            </div>
            <div className="meta">
              <span>🔔 已推送 {r.alertCount} 条</span>
              <span style={{ cursor: 'pointer', color: '#0B6E6B' }} onClick={() => push(r.id)}>📤 推送高危预警</span>
              <span style={{ cursor: 'pointer', color: '#EF4444' }} onClick={() => unbind(r.id)}>解绑</span>
            </div>
          </div>
        ))}
      </div>

      <div className="appbar plain"><div className="brand" style={{ fontSize: 15 }}>守护预警 · 时间线</div></div>
      <div className="list">
        {ov.alerts.length === 0 && <div className="empty">暂无预警，点“一键守护扫描”按订阅领域生成。</div>}
        {ov.alerts.map((a) => (
          <div className={'card alert ' + (a.readFlag ? '' : 'unread')} key={a.id}>
            <div className="top">
              <span className={'haz ' + lv(a.level)}><span className="dot" />预警</span>
              <b style={{ fontSize: 13 }}>{a.memberName} · {a.topic}</b>
              <span className="src">{(a.createdAt || '').slice(5, 16).replace('T', ' ')}</span>
            </div>
            <div style={{ fontSize: 13, color: '#334155', margin: '4px 0', lineHeight: 1.6 }}>{a.body}</div>
            <div className="meta">
              <span style={{ cursor: 'pointer', color: '#0B6E6B' }} onClick={() => speak(a.body)}>🔊 朗读播报</span>
              {!a.readFlag && <span style={{ cursor: 'pointer' }} onClick={() => markRead(a.id)}>✓ 标为已读</span>}
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
