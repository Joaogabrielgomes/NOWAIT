import { CircleHelp } from 'lucide-react'

export default function CampoAjuda({ texto }) {
  return (
    <span className="campo-ajuda" tabIndex={0}>
      <CircleHelp size={15} />
      <span className="campo-ajuda-tooltip">{texto}</span>
    </span>
  )
}
