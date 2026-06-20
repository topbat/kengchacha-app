import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { apiGet } from '../api'
import { speak } from '../voice'

type Badge = { id: number; name: string; icon: string; level: number; needScore: number; owned: boolean }
type Growth = {
  nickname: string; contribScore: number; level: number
  currentBadge: string; validStoryCnt: number; badges: Badge[]
}

export default function Me() {
  const [g, setG] = useState<Growth | null>(null)
  const [err, setErr] = useState('')
  const nav = useNavigate()
  useEffect(() => { apiGet('/growth/me').then(setG).catch((e) => setErr(e.message)) }, [])

  const entries = [
    { ic: '📋', label: '我的上报 · 审核', act: () => nav('/report') },
    { ic: '🧾', label: '检测记录（私密）', act: () => nav('/toolbox') },
    { ic: '⭐', label: '我的收藏', act: () => nav('/') },
    { ic: '👵', label: '家人守护', act: () => nav('/guardian') },
    { ic: '🔊', label: '适老化 / 语音', act: () => speak('坑查查适老化语音播报已开启。遇到转账、验证码，先挂断，拨打 96110 核实。') },
    { ic: '⚙️', label: '隐私与设置', act: () => speak('隐私设置：检测记录仅本地脱敏保存，不上传原文。') },
  ]

  if (err) return <div className="err">{err}</div>
  if (!g) return <div className="loading">加载中…</div>

  return (
    <div>
      <div className="profilehead">
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <div style={{ width: 52, height: 52, borderRadius: '50%', background: 'rgba(255,255,255,.2)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 26 }}>🛡️</div>
          <div>
            <div style={{ fontWeight: 800, fontSize: 17, display: 'flex', alignItems: 'center', gap: 6 }}>
              {g.nickname}
              <span style={{ fontSize: 10, background: '#fff', color: '#0B6E6B', borderRadius: 6, padding: '1px 6px' }}>{g.currentBadge}</span>
            </div>
            <div style={{ fontSize: 12, opacity: .9 }}>防坑贡献值 {g.contribScore} · 有效上报 {g.validStoryCnt} 条</div>
          </div>
        </div>
      </div>

      <div className="appbar plain"><div className="brand" style={{ fontSize: 15 }}>我的身份徽章墙</div></div>
      <div className="badges">
        {g.badges.map((b) => (
          <div className="bdg" key={b.id}>
            <div className={'ring' + (b.owned ? '' : ' lock')}>{b.owned ? b.icon : '🔒'}</div>
            <span>{b.name}</span>
          </div>
        ))}
      </div>

      <div className="list">
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
          {entries.map((e) => (
            <div className="card" key={e.label} style={{ margin: 0, textAlign: 'center', cursor: 'pointer' }} onClick={e.act}>
              <div style={{ fontSize: 22 }}>{e.ic}</div>
              <div style={{ fontSize: 12, color: '#475569', marginTop: 4 }}>{e.label}</div>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}
