import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

/**
 * Conecta ao canal STOMP/SockJS do NOWAIT e inscreve no tópico de fila de um
 * estabelecimento específico, chamando `onUpdate` sempre que o servidor
 * transmitir o estado atualizado da fila.
 *
 * Retorna uma função de "cleanup" que deve ser chamada ao desmontar o componente.
 */
export function conectarFila(estabelecimentoId, onUpdate) {
  const client = new Client({
    webSocketFactory: () => new SockJS('/ws'),
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
