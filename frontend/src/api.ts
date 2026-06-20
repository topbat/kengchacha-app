// 统一 API 封装：约定后端 { code, msg, data }，code=0 为成功。
const BASE = '/api'

async function handle(res: Response) {
  const json = await res.json()
  if (json.code !== 0) throw new Error(json.msg || '请求失败')
  return json.data
}

export async function apiGet(path: string) {
  const res = await fetch(BASE + path)
  return handle(res)
}

export async function apiPost(path: string, body?: unknown) {
  const res = await fetch(BASE + path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: body ? JSON.stringify(body) : undefined,
  })
  return handle(res)
}

// 危害等级 -> 展示
export const hazard = (lv?: number) =>
  lv === 3 ? { cls: 'r', txt: '高危' } : lv === 2 ? { cls: 'a', txt: '中危' } : { cls: 'g', txt: '低危' }

export const sourceLabel = (t?: number) =>
  t === 2 ? '官方预警' : t === 3 ? '判例投诉' : '真实经历'
