export type Session = { accessToken: string; refreshToken: string; user: { displayName: string; role: string; merchantId: string } }
const API = import.meta.env.VITE_API_BASE || '/api/v1'
export function session(): Session | null { const raw = localStorage.getItem('yike_session'); return raw ? JSON.parse(raw) : null }
export function saveSession(value: Session | null) { value ? localStorage.setItem('yike_session', JSON.stringify(value)) : localStorage.removeItem('yike_session') }
export async function request<T = any>(path: string, options: RequestInit = {}): Promise<T> {
  const token = session()?.accessToken
  const response = await fetch(`${API}${path}`, { ...options, headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}), ...options.headers } })
  const body = await response.json().catch(() => ({ message: '服务器响应异常' }))
  if (!response.ok) throw new Error(body.message || `请求失败 (${response.status})`)
  return body.data
}
export async function upload(file: File): Promise<{ url: string }> {
  const form = new FormData(); form.append('file', file)
  const response = await fetch(`${API}/uploads/images`, { method: 'POST', headers: { Authorization: `Bearer ${session()?.accessToken}` }, body: form })
  const body = await response.json(); if (!response.ok) throw new Error(body.message); return body.data
}
