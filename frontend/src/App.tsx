import { NavLink, Route, Routes } from 'react-router-dom'
import Feed from './pages/Feed'
import Toolbox from './pages/Toolbox'
import Assistant from './pages/Assistant'
import Quiz from './pages/Quiz'
import Report from './pages/Report'
import Me from './pages/Me'
import Guardian from './pages/Guardian'

const tabs = [
  { to: '/', ic: '📰', label: '头条', end: true },
  { to: '/toolbox', ic: '🧰', label: '工具箱' },
  { to: '/assistant', ic: '💬', label: '助手' },
  { to: '/quiz', ic: '🧭', label: '自测' },
  { to: '/report', ic: '✍️', label: '上报' },
  { to: '/me', ic: '👤', label: '我的' },
]
// 侧栏（PC 端）额外入口
const extra = [{ to: '/guardian', ic: '👵', label: '家人守护', end: false }]

export default function App() {
  return (
    <div className="app">
      {/* 桌面侧边导航（PC 端，移动端 CSS 隐藏） */}
      <aside className="sidenav">
        <div className="sn-brand"><span>🛡️</span><div><b>坑查查</b><small>有坑没坑 · 先查查</small></div></div>
        <nav>
          {[...tabs, ...extra].map((t) => (
            <NavLink key={t.to} to={t.to} end={t.end}
                     className={({ isActive }) => 'sn-item' + (isActive ? ' on' : '')}>
              <span className="ic">{t.ic}</span>{t.label}
            </NavLink>
          ))}
        </nav>
        <div className="sn-foot">反诈专线 96110 · 消费维权 12315<br />PC 端 · 内容为科普改写示意</div>
      </aside>

      <div className="view">
        <Routes>
          <Route path="/" element={<Feed />} />
          <Route path="/toolbox" element={<Toolbox />} />
          <Route path="/assistant" element={<Assistant />} />
          <Route path="/quiz" element={<Quiz />} />
          <Route path="/report" element={<Report />} />
          <Route path="/guardian" element={<Guardian />} />
          <Route path="/me" element={<Me />} />
        </Routes>
      </div>

      {/* 移动端底部 TabBar（PC 端 CSS 隐藏） */}
      <nav className="tabbar">
        {tabs.map((t) => (
          <NavLink key={t.to} to={t.to} end={t.end}
                   className={({ isActive }) => 'tab' + (isActive ? ' on' : '')}>
            <span className="ic">{t.ic}</span>{t.label}
          </NavLink>
        ))}
      </nav>
    </div>
  )
}
