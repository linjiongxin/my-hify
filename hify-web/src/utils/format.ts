/**
 * 格式化日期时间
 */
export function formatDateTime(val: unknown): string {
  if (!val) return '-'
  const d = new Date(String(val))
  if (Number.isNaN(d.getTime())) return String(val)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}