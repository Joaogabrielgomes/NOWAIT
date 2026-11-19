import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

const WS_URL = import.meta.env.VITE_WS_URL || '/ws'

export function conectarFila(estabelecimentoId, onUpdate) {
  const client = new Client({
    webSocketFactory: () => new SockJS(WS_URL),
    reconnectDelay: 4000,
    onConnect: () => {
      client.subscribe(`/topic/fila.${estabelecimentoId}`, (message) => {
        try {
          onUpdate(JSON.parse(message.body))
        } catch (e) {
          console.error('Falha ao processar atualização da fila', e)
        }
      })
    },
  })

  client.activate()

  return () => {
    client.deactivate()
  }
}
