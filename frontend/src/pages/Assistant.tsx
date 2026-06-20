import { useState } from 'react'
import { apiPost } from '../api'

type Ref = { id: number; title: string; slogan: string; hazardLevel: number }
type Answer = {
  judgement: string; trickType: string; deduction: string
  steps: string[]; references: Ref[]; aiSummary: string; disclaimer: string
}
type Msg = { role: 'u' } & { text: string } | ({ role: 'a' } & Answer)

const EXAMPLES = [
  '我妈被拉进一个荐股群，还说能内部抢原始股',
  '注销校园贷会影响征信吗',
  '兼职打字员要我先交298押金',
  '接到电话说我涉案要转到安全账户',
]

export default function Assistant() {
  const [msgs, setMsgs] = useState<Msg[]>([])
  const [text, setText] = useState('')
  const [loading, setLoading] = useState(false)

  async function send(q: string) {
    const msg = q.trim()
    if (!msg || loading) return
    setMsgs((m) => [...m, { role: 'u', text: msg } as Msg])
    setText('')
    setLoading(true)
    try {
      const ans: Answer = await apiPost('/assistant/chat', { message: msg, inputType: 'text' })
      setMsgs((m) => [...m, { role: 'a', ...ans } as Msg])
    } catch (e: any) {
      setMsgs((m) => [...m, { role: 'a', judgement: '出错了：' + e.message, trickType: '', deduction: '', steps: [], references: [], aiSummary: '', disclaimer: '' } as Msg])
    } finally { setLoading(false) }
  }

  return (
    <div>
      <div className="appbar plain">
        <div className="brand">💬 AI 避坑助手</div>
        <div className="sub">可说话 · 可拍照 · 可贴链接（演示以文字为主）</div>
      </div>

      <div className="chat">
        {msgs.length === 0 && (
          <div className="msg a">
            <div className="seg">你好，我是坑查查 AI 助手。把你遇到的情况告诉我，我帮你判断是不是坑、该怎么办。</div>
          </div>
        )}
        {msgs.map((m, i) =>
          m.role === 'u' ? (
            <div className="msg u" key={i}>{(m as any).text}</div>
          ) : (
            <div className="msg a" key={i}>
              <div className="jt">{(m as any).judgement}</div>
              {(m as any).trickType && <div className="seg"><b>套路类型：</b>{(m as any).trickType}</div>}
              {(m as any).deduction && <div className="seg"><b>套路推演：</b>{(m as any).deduction}</div>}
              {(m as any).steps?.length > 0 && (
                <div className="seg"><b>你该怎么做：</b>
                  <ol style={{ margin: '4px 0 0 18px' }}>
                    {(m as any).steps.map((s: string, j: number) => <li key={j}>{s}</li>)}
                  </ol>
                </div>
              )}
              {(m as any).references?.map((r: Ref) => (
                <div className="ref" key={r.id}>📎 关联案例：{r.title} —— {r.slogan}</div>
              ))}
              {(m as any).aiSummary && <div className="seg" style={{ color: '#0B6E6B' }}>{(m as any).aiSummary}</div>}
              {(m as any).disclaimer && <div className="disc">{(m as any).disclaimer}</div>}
            </div>
          )
        )}
        {loading && <div className="msg a">正在分析…</div>}
      </div>

      <div className="quickrow">
        {EXAMPLES.map((e) => <span className="quick" key={e} onClick={() => send(e)}>{e}</span>)}
      </div>

      <div className="inputbar">
        <input value={text} placeholder="描述你的情况…" onChange={(e) => setText(e.target.value)}
               onKeyDown={(e) => e.key === 'Enter' && send(text)} />
        <button onClick={() => send(text)} disabled={loading}>发送</button>
      </div>
    </div>
  )
}
