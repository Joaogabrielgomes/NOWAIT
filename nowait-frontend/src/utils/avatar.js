
const CORES = ['#ef5a1f', '#4f9c3a', '#e2402a', '#e8ac1d', '#d1652c', '#82a334', '#dd8f22']

export function corAvatar(texto) {
  const base = texto || ''
  let hash = 0
  for (let i = 0; i < base.length; i += 1) {
    hash = base.charCodeAt(i) + ((hash << 5) - hash)
  }
  return CORES[Math.abs(hash) % CORES.length]
}
