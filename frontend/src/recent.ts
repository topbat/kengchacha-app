// 个性化足迹（本地）：近期浏览内容 id + 兴趣标签，喂给「为你推荐」构造兴趣向量。
const R_KEY = 'kcc_recent'
const I_KEY = 'kcc_interests'

function read(key: string): any[] {
  try { return JSON.parse(localStorage.getItem(key) || '[]') } catch { return [] }
}

export function getRecent(): number[] {
  return read(R_KEY).filter((x) => typeof x === 'number')
}

export function pushRecent(id: number) {
  const arr = getRecent().filter((x) => x !== id)
  arr.unshift(id)
  localStorage.setItem(R_KEY, JSON.stringify(arr.slice(0, 20)))
}

export function getInterests(): string[] {
  return read(I_KEY).filter((x) => typeof x === 'string')
}

export function addInterest(tag: string) {
  if (!tag) return
  const set = new Set(getInterests())
  set.add(tag)
  localStorage.setItem(I_KEY, JSON.stringify([...set].slice(0, 10)))
}
