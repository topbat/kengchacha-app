import { useEffect, useState } from 'react'
import { apiGet, apiPost, hazard, sourceLabel } from '../api'
import { asrSupported, listenOnce, speak } from '../voice'
import { addInterest, getInterests, getRecent, pushRecent } from '../recent'
import PosterModal from '../components/PosterModal'

type Card = {
  id: number; title: string; victimGroup: string; trick: string; loss: string
  tip: string; slogan: string; hazardLevel: number; sourceType: number
  sourceRef: string; hotScore: number; tags: string[]
}
type Rec = { id: number; title: string; slogan: string; trick: string; hazardLevel: number; hotScore: number; tags: string[]; similarity: number; reason: string }
type TagGroup = { dimension: number; dimensionName: string; tags: string[] }

const DIM_KEY: Record<number, string> = { 1: 'stage', 2: 'domain', 3: 'psych' }

function fmt(s: number) {
  const h = Math.floor(s / 3600), m = Math.floor((s % 3600) / 60), sec = s % 60
  const p = (n: number) => String(n).padStart(2, '0')
  return `${p(h)}:${p(m)}:${p(sec)}`
}

export default function Feed() {
  const [groups, setGroups] = useState<TagGroup[]>([])
  const [filters, setFilters] = useState<Record<string, string>>({})
  const [items, setItems] = useState<Card[]>([])
  const [recs, setRecs] = useState<Rec[]>([])
  const [similar, setSimilar] = useState<Record<number, Rec[]>>({})
  const [poster, setPoster] = useState<number | null>(null)
  const [q, setQ] = useState('')
  const [left, setLeft] = useState(0)
  const [err, setErr] = useState('')
  const [listening, setListening] = useState(false)

  useEffect(() => { apiGet('/content/tags').then(setGroups).catch((e) => setErr(e.message)) }, [])
  useEffect(() => { apiGet('/refresh/countdown').then((d) => setLeft(d.secondsLeft)) }, [])
  useEffect(() => { const t = setInterval(() => setLeft((s) => (s > 0 ? s - 1 : 0)), 1000); return () => clearInterval(t) }, [])
  useEffect(() => { load() }, [filters])
  useEffect(() => { loadRecs() }, [])

  function load() {
    const p = new URLSearchParams()
    Object.entries(filters).forEach(([k, v]) => { if (v) p.set(k, String(v)) })
    p.set('size', '20')
    apiGet('/content/feed?' + p.toString()).then((d) => setItems(d.items)).catch((e) => setErr(e.message))
  }
  function loadRecs() {
    apiPost('/recommend/for-you', { recentIds: getRecent(), interests: getInterests(), size: 6 })
      .then(setRecs).catch(() => {})
  }
  function toggle(key: string, val: string) {
    if (key === 'domain') addInterest(val)   // 选领域=表达兴趣
    setFilters((f) => (f[key] === val ? { ...f, [key]: '' } : { ...f, [key]: val }))
  }

  function visit(id: number, tag?: string) { pushRecent(id); if (tag) addInterest(tag) }

  function listen(c: Card) {
    speak(`${c.title}。坑人套路：${c.trick}。避坑常识：${c.tip}`)
    visit(c.id)
  }
  async function showSimilar(c: Card) {
    visit(c.id, c.tags[0])
    if (similar[c.id]) { setSimilar((s) => { const n = { ...s }; delete n[c.id]; return n }); return }
    try { const list: Rec[] = await apiGet(`/recommend/similar/${c.id}?size=4`); setSimilar((s) => ({ ...s, [c.id]: list })) } catch {}
  }
  function openPoster(c: Card) { visit(c.id); setPoster(c.id) }

  async function micSearch() {
    if (!asrSupported()) { setErr('当前浏览器不支持语音搜索，请用键盘'); return }
    setListening(true); setErr('')
    try {
      const text = await listenOnce({ interim: (t) => setQ(t) })
      if (text) { setQ(text); setFilters((f) => ({ ...f, q: text })) }
    } catch (e: any) { setErr(e.message) }
    finally { setListening(false) }
  }

  const h = hazard
  return (
    <div>
      <div className="appbar">
        <div className="brand"><span>🛡️</span> 坑查查 · 避坑头条</div>
        <div className="sub">每 2 小时滚动更新 · 真实经历 + 官方预警</div>
        <div className="searchbar">
          <span>🔍</span>
          <input value={q} placeholder="说出 / 输入你想避的坑…"
                 onChange={(e) => setQ(e.target.value)}
                 onKeyDown={(e) => e.key === 'Enter' && setFilters((f) => ({ ...f, q }))} />
          <span className={'mic' + (listening ? ' on' : '')} title="语音搜索" onClick={micSearch}>{listening ? '●' : '🎤'}</span>
        </div>
      </div>

      <div className="countdown">
        <span className="lab">距下次内容更新</span>
        <span className="t">{fmt(left)}</span>
      </div>

      {recs.length > 0 && (
        <div className="rec-sec">
          <div className="rec-head">✨ 为你推荐 <span>按你的浏览与兴趣 · 语义召回</span></div>
          <div className="rec-row">
            {recs.map((r) => {
              const hz = h(r.hazardLevel)
              return (
                <div className="rec-card" key={r.id} onClick={() => { visit(r.id, r.tags[0]); loadRecs() }}>
                  <div className="rec-top"><span className={'haz ' + hz.cls}>{hz.txt}</span><span className="sim">{r.similarity}%</span></div>
                  <div className="rec-ttl">{r.title}</div>
                  <div className="rec-why">{r.reason}</div>
                </div>
              )
            })}
          </div>
        </div>
      )}

      <div className="filters">
        {groups.filter((g) => DIM_KEY[g.dimension]).map((g) => (
          <div className="filter-group" key={g.dimension}>
            <div className="gl">{g.dimensionName}</div>
            <div className="chiprow">
              {g.tags.map((t) => {
                const key = DIM_KEY[g.dimension]
                const on = filters[key] === t
                return <span key={t} className={'chip' + (on ? ' on' : '')} onClick={() => toggle(key, t)}>{t}</span>
              })}
            </div>
          </div>
        ))}
        <div className="filter-group">
          <div className="gl">危害等级</div>
          <div className="chiprow">
            {[['3', '🔴 高危'], ['2', '🟡 中危'], ['1', '🟢 低危']].map(([v, label]) => (
              <span key={v} className={'chip' + (filters.hazard === v ? ' on' : '')} onClick={() => toggle('hazard', v)}>{label}</span>
            ))}
          </div>
        </div>
      </div>

      {err && <div className="err">{err}</div>}

      <div className="list">
        {items.length === 0 && !err && <div className="empty">没有匹配的内容，换个筛选试试～</div>}
        {items.map((c) => {
          const hz = h(c.hazardLevel)
          const sim = similar[c.id]
          return (
            <div className="card" key={c.id}>
              <div className="top">
                <span className={'haz ' + hz.cls}><span className="dot" />{hz.txt}</span>
                <span className="src">{sourceLabel(c.sourceType)} · {c.sourceRef}</span>
              </div>
              <div className="ttl">{c.title}</div>
              <div className="elem">
                <span className="k">踩坑人群</span><span className="v">{c.victimGroup}</span>
                <span className="k">坑人套路</span><span className="v">{c.trick}</span>
                <span className="k">损失后果</span><span className="v">{c.loss}</span>
                <span className="k">避坑常识</span><span className="v">{c.tip}</span>
              </div>
              <div className="slogan">💡 {c.slogan}</div>
              <div className="tags">{c.tags.map((t) => <span className="tg" key={t}>#{t}</span>)}</div>
              <div className="meta">
                <span>🔥 热度 {c.hotScore}</span>
                <span className="act" onClick={() => listen(c)}>🔊 听一听</span>
                <span className="act" onClick={() => showSimilar(c)}>🔗 相似坑</span>
                <span className="act" onClick={() => openPoster(c)}>🖼 海报</span>
              </div>
              {sim && (
                <div className="sim-box">
                  <div className="sim-h">相似案例（语义近邻）</div>
                  {sim.length === 0 && <div className="empty" style={{ padding: 10 }}>暂无相似案例</div>}
                  {sim.map((s) => (
                    <div className="sim-item" key={s.id}>
                      <span className={'dotc ' + h(s.hazardLevel).cls} />
                      <span className="sim-t">{s.title}</span>
                      <span className="sim-p">{s.similarity}%</span>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )
        })}
      </div>

      {poster !== null && <PosterModal contentId={poster} onClose={() => setPoster(null)} />}
    </div>
  )
}
