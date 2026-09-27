const TEXTO = `POLÍTICA DE PRIVACIDADE — NOWAIT

Este documento estabelece as diretrizes de tratamento, proteção, armazenamento, retenção e descarte de dados pessoais no sistema NOWAIT, plataforma web para gestão de filas de espera virtuais, agendamentos e alocação de mesas em restaurantes. Os procedimentos aqui descritos seguem a Lei Geral de Proteção de Dados Pessoais (Lei nº 13.709/2018).

FINALIDADE DO SISTEMA
O NOWAIT tem como finalidade conectar clientes e restaurantes por meio de cadastro e autenticação de usuários, cadastro de estabelecimentos e mapa de mesas, gestão de fila de espera virtual em tempo real, agendamento de horários com cancelamento automático por tolerância, e alocação automatizada de mesas por capacidade. O sistema possui três perfis de acesso: Cliente, Estabelecimento e Administrador, cada um com permissões específicas.

DADOS PESSOAIS COLETADOS
Usuário: nome completo, e-mail e senha, armazenada com hash.
Estabelecimento (cadastro do restaurante): nome do estabelecimento e endereço (CEP, logradouro, número, complemento, bairro, cidade, estado).
Gerados pelo sistema: registro de fila de espera, registro de agendamento e mesas por estabelecimento.
Nenhum dado sensível é coletado. Não são coletados CPF, RG, data de nascimento, telefone ou dados de pagamento.

FINALIDADE DO TRATAMENTO DE DADOS
Os dados pessoais são utilizados exclusivamente para identificação, cadastro e autenticação de usuários; operação da fila de espera e dos agendamentos; e para exibição de estabelecimentos e sua localização.

COMPARTILHAMENTO DE DADOS
Os dados pessoais tratados pelo NOWAIT não são vendidos nem cedidos para fins comerciais. O compartilhamento ocorre exclusivamente nas situações abaixo.

Integração com API ViaCEP: utilizada para cadastrar o estabelecimento e completar automaticamente o endereço a partir apenas do CEP informado.

Redefinição de senha: quando o usuário solicita a troca de senha, um link de redefinição é enviado para o e-mail cadastrado.

Prestadores de infraestrutura: serviços de hospedagem e banco de dados podem ter acesso técnico limitado restrito ao necessário para manter o sistema em funcionamento.

Outras situações previstas em lei: mediante solicitação formal por autoridade competente.

TRANSFERÊNCIA INTERNACIONAL DE DADOS
O provedor de envio de e-mails utilizado para o reset de senha pode processar dados fora do Brasil, sob as garantias contratuais do próprio fornecedor. Nenhum outro dado é enviado internacionalmente.

COOKIES
O NOWAIT não utiliza cookies de rastreamento ou publicidade. A sessão do usuário é mantida por token de autenticação (JWT) armazenado localmente no navegador.

ARMAZENAMENTO E SEGURANÇA DA INFORMAÇÃO
Senhas protegidas com hash (BCrypt); autenticação por token (JWT) com expiração; autorização verificada no backend em cada operação, por perfil de usuário; acesso a dados de um estabelecimento restrito ao seu próprio dono; limitação de requisições contra abuso técnico; e trilha de auditoria das operações sensíveis do sistema.

RETENÇÃO DE DADOS
Conta: mantida até que seja solicitada exclusão ou anonimização pelo usuário. Registros de fila e agendamento: enquanto a conta estiver logada. Logs de auditoria: mantidos por 12 meses e removidos automaticamente após esse prazo.

EXCLUSÃO E ANONIMIZAÇÃO DE DADOS
O usuário pode solicitar a exclusão de sua conta a qualquer momento. Com entradas de fila ou agendamentos realizados, os dados de identificação são removidos e os registros são mantidos de forma anônima.

DIREITOS DO TITULAR DOS DADOS
Conforme a LGPD, o usuário pode solicitar acesso aos dados e tratamento, correção de dados incompletos, exclusão ou anonimização da conta, informação sobre compartilhamentos realizados e revogação de consentimentos, quando aplicável.

RESPOSTA A INCIDENTES DE SEGURANÇA
Em caso de incidente que envolva risco relevante aos usuários, o incidente será avaliado e, se necessário, comunicado à ANPD e aos usuários afetados, conforme exigido pela LGPD.

DISPOSIÇÕES GERAIS
Esta política pode ser atualizada periodicamente, persistindo sempre a versão mais recente. O uso do sistema representa concordância com as diretrizes aqui descritas.

CONTATO PARA ASSUNTOS DE PRIVACIDADE
E-mail: nowait.noreply@gmail.com

DECLARAÇÃO DE CONCORDÂNCIA E CIÊNCIA
Ao criar uma conta no NOWAIT, o usuário declara estar ciente e de acordo com esta Política de Privacidade, e reconhecendo que o tratamento de dados ocorre apenas para as finalidades aqui descritas.


TERMOS DE USO — NOWAIT

INTRODUÇÃO
Estes Termos de Uso regulam a utilização do sistema NOWAIT, protótipo acadêmico de gestão de filas de espera, agendamentos e alocação de mesas para restaurantes. Ao criar uma conta, o usuário concorda com estas condições e com a Política de Privacidade do sistema.

QUEM PODE USAR
O uso do NOWAIT é destinado a maiores de 18 anos com capacidade civil plena.

CONTAS DE USUÁRIO
Existem dois perfis de conta: Cliente e Estabelecimento. Cada estabelecimento é vinculado a um único usuário responsável. O usuário é responsável por fornecer informações verdadeiras, manter sua senha em sigilo e comunicar imediatamente qualquer uso não autorizado da conta.

CONDUTAS PROIBIDAS
É proibido fornecer dados de identificação falsos, utilizar a fila ou o agendamento de forma abusiva sem intenção de comparecer ao estabelecimento, tentar acessar dados de outros usuários, ou tentar abusar de falhas no sistema. O NOWAIT pode suspender ou encerrar contas que violem estas regras.

FUNCIONAMENTO DO SERVIÇO
A posição na fila e o tempo estimado de espera são calculados automaticamente e podem variar conforme o estabelecimento. Agendamentos possuem tolerância de atraso definida pelo sistema, após esse prazo, o agendamento é cancelado automaticamente. A alocação de mesas segue um algoritmo automático por capacidade, podendo o estabelecimento alocar clientes manualmente por questões internas.

DISPONIBILIDADE
O NOWAIT é oferecido conforme disponibilidade técnica, podendo sofrer de interrupções para manutenção ou por motivos fora do controle do sistema.

ENCERRAMENTO DE CONTA
O usuário pode solicitar o encerramento de sua conta a qualquer momento.

LIMITAÇÃO DE RESPONSABILIDADE
O NOWAIT atua como intermediário entre clientes e estabelecimentos, não sendo responsável pela qualidade do atendimento local de cada restaurante.

ALTERAÇÕES
Estes Termos podem ser atualizados periodicamente, prevalecendo a versão mais recente.

LEI APLICÁVEL
Estes Termos são regidos pela legislação brasileira.

DECLARAÇÃO DE CONCORDÂNCIA
Ao utilizar o NOWAIT, o usuário declara estar ciente e de acordo com todas as disposições destes Termos de Uso.`

export default function TermosModal({ onClose }) {
  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()}>
        <button className="modal-close" onClick={onClose} aria-label="Fechar">×</button>
        <div className="modal-body">
          <h2>Termos de Uso e Política de Privacidade</h2>
          {TEXTO}
        </div>
      </div>
    </div>
  )
}
