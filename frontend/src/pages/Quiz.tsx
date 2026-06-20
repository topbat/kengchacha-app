import { useState } from 'react'
import { apiGet, apiPost } from '../api'

type Option = { id: number; content: string }
type Question = { id: number; stem: string; dimension: string; options: Option[] }
type DimScore = { dimension: string; correct: number; total: number; rate: number }
type Result = {
  score: number; correctCount: number; total: number; profileTitle: string
  profileTags: string[]; dimScores: DimScore[]; riskScenes: string[]; advice: string[]
}

export default function Quiz() {
  const [phase, setPhase] = useState<'intro' | 'doing' | 'result'>('intro')
  const [questions, setQuestions] = useState<Question[]>([])
  const [picked, setPicked] = useState<Record<number, number>>({})
  const [result, setResult] = useState<Result | null>(null)
  const [busy, setBusy] = useState(false)

  async function start(scale: number) {
    setBusy(true)
    try {
      const d = await apiGet(`/quiz/start?scale=${scale}&mode=1`)
      setQuestions(d.questions); setPicked({}); setPhase('doing')
    } finally { setBusy(false) }
  }
  async function submit() {
    const answers = Object.entries(picked).map(([qid, oid]) => ({ questionId: Number(qid), optionId: oid }))
    if (answers.length < questions.length) { alert('还有题没答完哦～'); return }
    setBusy(true)
    try {
      const d = await apiPost('/quiz/submit', { scale: questions.length, mode: 1, answers })
      setResult(d); setPhase('result')
    } finally { setBusy(false) }
  }

  if (phase === 'intro') return (
    <div>
      <div className="appbar plain"><div className="brand">🧭 防坑能力自测</div><div className="sub">测出你最容易踩的坑，给出可执行建议</div></div>
      <div className="hero"><h3>你会中什么坑？</h3><p>覆盖心理 / 法律 / 消费 / 金融 / 职场 / 网络六大维度</p></div>
      <div className="btn-row"><button className="btn" disabled={busy} onClick={() => start(10)}>快测 10 题</button></div>
      <div className="btn-row">
        <button className="btn ghost" disabled={busy} onClick={() => start(30)}>标准 30 题</button>
        <button className="btn ghost" disabled={busy} onClick={() => start(50)}>深度 50 题</button>
      </div>
      <div className="empty">题量以题库为准，种子题库较小时将返回全部题目。</div>
    </div>
  )

  if (phase === 'doing') return (
    <div>
      <div className="appbar plain"><div className="brand">答题中 · 共 {questions.length} 题</div><div className="sub">已答 {Object.keys(picked).length} / {questions.length}</div></div>
      {questions.map((q, i) => (
        <div className="q" key={q.id}>
          <div className="stem">{i + 1}. {q.stem} <span style={{ color: '#94A3B8', fontWeight: 400 }}>[{q.dimension}]</span></div>
          {q.options.map((o) => (
            <div key={o.id} className={'opt' + (picked[q.id] === o.id ? ' sel' : '')}
                 onClick={() => setPicked((p) => ({ ...p, [q.id]: o.id }))}>{o.content}</div>
          ))}
        </div>
      ))}
      <div className="btn-row"><button className="btn" disabled={busy} onClick={submit}>提交，看我的防坑画像</button></div>
    </div>
  )

  // result
  const r = result!
  return (
    <div>
      <div className="appbar plain"><div className="brand">我的防坑画像</div><div className="sub">默认私密 🔒 仅你可见</div></div>
      <div className="hero">
        <h3>「{r.profileTitle}」</h3>
        <p>防坑总分 <b style={{ fontSize: 22 }}>{r.score}</b> / 100 · 答对 {r.correctCount}/{r.total}</p>
      </div>

      <div className="q">
        <div className="stem">六维能力</div>
        {r.dimScores.map((d) => (
          <div className="dimbar" key={d.dimension}>
            <div className="h"><span>{d.dimension}</span><span>{d.rate}%（{d.correct}/{d.total}）</span></div>
            <div className="track"><div className="fill" style={{ width: d.rate + '%' }} /></div>
          </div>
        ))}
      </div>

      <div className="q">
        <div className="stem">⚡ 你最可能踩的坑</div>
        {r.riskScenes.map((s, i) => <div key={i} style={{ fontSize: 13, color: '#475569', margin: '6px 0' }}>· {s}</div>)}
      </div>

      <div className="q">
        <div className="stem">🛡️ 给你的避坑动作</div>
        {r.advice.map((a, i) => <div key={i} style={{ fontSize: 13, color: '#475569', margin: '6px 0' }}>✓ {a}</div>)}
      </div>

      <div className="btn-row">
        <button className="btn ghost" onClick={() => setPhase('intro')}>再测一次</button>
        <button className="btn" onClick={() => alert('已生成分享卡片（演示）')}>生成分享卡片</button>
      </div>
    </div>
  )
}
