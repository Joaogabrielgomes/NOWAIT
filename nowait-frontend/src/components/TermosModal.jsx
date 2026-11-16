const TEXTO = `POLÍTICA DE PRIVACIDADE — NOWAIT

Este documento estabelece as diretrizes de tratamento, proteção, armazenamento, retenção e descarte de dados pessoais no sistema NOWAIT, plataforma web para gestão de filas de espera virtuais, agendamentos e alocação de mesas em restaurantes. Os procedimentos aqui descritos seguem a Lei Geral de Proteção de Dados Pessoais (Lei nº 13.709/2018).

FINALIDADE DO SISTEMA
O NOWAIT tem como finalidade conectar clientes e restaurantes por meio de cadastro e autenticação de usuários, cadastro de estabelecimentos e mapa de mesas, gestão de fila de espera virtual em tempo real, agendamento de horários com cancelamento automático por tolerância, e alocação automatizada de mesas por capacidade. O sistema possui três perfis de acesso: Cliente, Estabelecimento e Administrador, cada um com permissões específicas.

DADOS PESSOAIS COLETADOS
Usuário: nome completo, e-mail e senha, armazenada com hash.
Estabelecimento (cadastro do restaurante): nome do estabelecimento e endereço (CEP, logradouro, número, complemento, bairro, cidade, estado).
Gerados pelo sistema: registro de fila de espera, registro de agendamento, favoritos e mesas por estabelecimento.
Denúncias: motivo informado pelo denunciante, tipo e identificador do conteúdo ou usuário denunciado, status da análise e observação registrada pelo administrador responsável pela decisão.
Nenhum dado sensível é coletado. Não são coletados CPF, RG, data de nascimento, telefone ou dados de pagamento.

FINALIDADE DO TRATAMENTO DE DADOS
Os dados pessoais são utilizados exclusivamente para identificação, cadastro e autenticação de usuários; operação da fila de espera e dos agendamentos; exibição de estabelecimentos e sua localização; e apuração de denúncias para manutenção da segurança e integridade da plataforma.

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
Conta: mantida até que seja solicitada exclusão ou anonimização pelo usuário. Registros de fila e agendamento: enquanto a conta estiver ativa. Logs de auditoria: mantidos por 12 meses e removidos automaticamente após esse prazo. Denúncias: mantidas enquanto necessário para fins de moderação, segurança e auditoria do sistema.

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
É proibido fornecer dados de identificação falsos, utilizar a fila ou o agendamento de forma abusiva sem intenção de comparecer ao estabelecimento, tentar acessar dados de outros usuários, ou tentar abusar de falhas no sistema. Também é proibido cadastrar nome de usuário ou de estabelecimento com conteúdo ofensivo, discriminatório ou impróprio, e fazer denúncias falsas, infundadas ou de má-fé contra outros usuários, estabelecimentos ou avaliações. O NOWAIT pode suspender ou encerrar contas que violem estas regras.

FUNCIONAMENTO DO SERVIÇO
A posição na fila e o tempo estimado de espera são calculados automaticamente e podem variar conforme o estabelecimento. Agendamentos possuem tolerância de atraso definida pelo sistema, após esse prazo, o agendamento é cancelado automaticamente. A alocação de mesas segue um algoritmo automático por capacidade, podendo o estabelecimento alocar clientes manualmente por questões internas. Para impedir conteúdo ofensivo, o sistema verifica automaticamente o nome informado no cadastro de usuário e de estabelecimento contra uma lista de termos proibidos, podendo recusar o cadastro quando um termo for identificado.

DENÚNCIAS E MODERAÇÃO DE CONTEÚDO
Clientes podem denunciar estabelecimentos e avaliações de outros usuários. Estabelecimentos podem denunciar outros estabelecimentos, avaliações e usuários (por exemplo, por uso abusivo da fila ou de agendamentos). Toda denúncia exige a indicação de um motivo e é analisada por um administrador do NOWAIT, que pode aprová-la — podendo remover o conteúdo denunciado e/ou banir o usuário responsável — ou rejeitá-la. O NOWAIT não garante prazo para a análise de uma denúncia nem se obriga a adotar a medida sugerida pelo denunciante, e a decisão do administrador é tomada com base nas informações disponíveis no momento da análise.

SUSPENSÃO E BANIMENTO DE CONTA
O NOWAIT pode banir uma conta, de forma direta pelo administrador ou em decorrência da aprovação de uma denúncia. A conta banida perde o acesso ao sistema até que o banimento seja revertido, a critério exclusivo do administrador. O banimento não gera direito a indenização.

DISPONIBILIDADE
O NOWAIT é oferecido conforme disponibilidade técnica, podendo sofrer interrupções para manutenção ou por motivos fora do controle do sistema.

ENCERRAMENTO DE CONTA
O usuário pode solicitar o encerramento de sua conta a qualquer momento, ressalvada a hipótese de banimento, que segue as regras da seção "Suspensão e banimento de conta".

LIMITAÇÃO DE RESPONSABILIDADE
O NOWAIT atua como intermediário entre clientes e estabelecimentos, não sendo responsável pela qualidade do atendimento local de cada restaurante. O NOWAIT também não se responsabiliza por: conteúdo gerado pelos próprios usuários, como avaliações, nomes de estabelecimento e denúncias; a veracidade de denúncias registradas por um usuário contra outro; danos decorrentes de denúncias infundadas ou de má-fé; decisões de aprovação, rejeição ou banimento tomadas na análise de uma denúncia; e eventual conteúdo ofensivo que não seja identificado pelo filtro automático de palavras proibidas, o qual não garante a filtragem de todo e qualquer conteúdo impróprio.

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
