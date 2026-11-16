package com.nowait.service;

import com.nowait.exception.InvalidInputException;
import jakarta.annotation.PostConstruct;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private static final String ARQUIVO_TEMPLATE_RESET       = "templates/email-reset-senha.html";
    private static final String ARQUIVO_TEMPLATE_NOTIFICACAO = "templates/email-notificacao.html";
    private static final String ARQUIVO_TEMPLATE_BANIMENTO   = "templates/email-banimento.html";
    private static final String ARQUIVO_TEMPLATE_REJEICAO_ESTABELECIMENTO = "templates/email-rejeicao-estabelecimento.html";
    private static final String PLACEHOLDER_LINK             = "{{link}}";
    private static final String PLACEHOLDER_TITULO           = "{{titulo}}";
    private static final String PLACEHOLDER_MENSAGEM         = "{{mensagem}}";
    private static final String PLACEHOLDER_MOTIVO           = "{{motivo}}";
    private static final String NOME_REMETENTE               = "NOWAIT";
    private static final String ASSUNTO_RESET                = "NOWAIT — Redefinição de senha";
    private static final String ASSUNTO_BANIMENTO            = "NOWAIT — Sua conta foi banida";
    private static final String ASSUNTO_DESBANIMENTO         = "NOWAIT — Seu banimento foi removido";
    private static final String ASSUNTO_REJEICAO_ESTABELECIMENTO = "NOWAIT — Seu estabelecimento foi rejeitado";
    private static final String MOTIVO_PADRAO                = "Não especificado";
    private static final DateTimeFormatter FORMATO_DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String remetente;

    @Value("${app.url}")
    private String appUrl;

    private String templateResetSenha;
    private String templateNotificacao;
    private String templateBanimento;
    private String templateRejeicaoEstabelecimento;

    @PostConstruct
    public void carregarTemplates() {
        templateResetSenha = carregarTemplate(ARQUIVO_TEMPLATE_RESET);
        templateNotificacao = carregarTemplate(ARQUIVO_TEMPLATE_NOTIFICACAO);
        templateBanimento = carregarTemplate(ARQUIVO_TEMPLATE_BANIMENTO);
        templateRejeicaoEstabelecimento = carregarTemplate(ARQUIVO_TEMPLATE_REJEICAO_ESTABELECIMENTO);
        log.info("Templates de e-mail carregados.");
    }

    public void enviarEmailResetSenha(String destinatario, String token) {
        String link  = construirLinkReset(token);
        String corpo = templateResetSenha.replace(PLACEHOLDER_LINK, link);
        enviarEmail(destinatario, ASSUNTO_RESET, corpo);
        log.info("E-mail de reset enviado para: {}", destinatario);
    }

    public void enviarEmailBanimento(String destinatario, String nomeUsuario, String motivo, String tokenContestacao) {
        String link  = construirLinkContestacaoBanimento(tokenContestacao);
        String corpo = templateBanimento
                .replace(PLACEHOLDER_LINK, link)
                .replace(PLACEHOLDER_MOTIVO, (motivo == null || motivo.isBlank()) ? MOTIVO_PADRAO : motivo);
        enviarEmail(destinatario, ASSUNTO_BANIMENTO, corpo);
        log.info("E-mail de banimento enviado para: {}", destinatario);
    }

    public void enviarEmailRejeicaoEstabelecimento(String destinatario, String nomeEstabelecimento,
                                                    String motivo, String tokenContestacao) {
        String link  = construirLinkContestacaoRejeicaoEstabelecimento(tokenContestacao);
        String corpo = templateRejeicaoEstabelecimento
                .replace(PLACEHOLDER_LINK, link)
                .replace(PLACEHOLDER_MOTIVO, (motivo == null || motivo.isBlank()) ? MOTIVO_PADRAO : motivo);
        enviarEmail(destinatario, ASSUNTO_REJEICAO_ESTABELECIMENTO, corpo);
        log.info("E-mail de rejeição de estabelecimento enviado para: {}", destinatario);
    }

    public void enviarEmailDesbanimento(String destinatario, String nomeUsuario) {
        String mensagem = String.format(
                "Olá, %s. O banimento da sua conta no NOWAIT foi removido por um administrador e você já "
                        + "pode fazer login normalmente.",
                nomeUsuario);
        enviarEmailNotificacao(destinatario, ASSUNTO_DESBANIMENTO, "Sua conta foi desbanida", mensagem);
    }

    public void enviarEmailAgendamentoCriado(String destinatario, String clienteNome,
                                              LocalDateTime dataHora, int quantidadePessoas) {
        String mensagem = String.format(
                "%s fez um agendamento para %s (%d pessoa(s)). Acesse o painel para confirmar ou recusar.",
                clienteNome, dataHora.format(FORMATO_DATA_HORA), quantidadePessoas);
        enviarEmailNotificacao(destinatario, "NOWAIT — Novo agendamento recebido", "Novo agendamento recebido", mensagem);
    }

    public void enviarEmailAgendamentoAceito(String destinatario, String estabelecimentoNome,
                                              String telefone, LocalDateTime dataHora) {
        String mensagem = String.format(
                "Seu agendamento no %s para %s foi confirmado. Qualquer dúvida, fale com o estabelecimento pelo telefone %s.",
                estabelecimentoNome, dataHora.format(FORMATO_DATA_HORA), telefone);
        enviarEmailNotificacao(destinatario, "NOWAIT — Agendamento confirmado", "Agendamento confirmado", mensagem);
    }

    public void enviarEmailAgendamentoRecusado(String destinatario, String estabelecimentoNome,
                                                String telefone, LocalDateTime dataHora, String motivo) {
        String mensagem = String.format(
                "Seu agendamento no %s para %s foi recusado. Motivo informado pelo estabelecimento: \"%s\". "
                        + "Qualquer dúvida, fale com o estabelecimento pelo telefone %s.",
                estabelecimentoNome, dataHora.format(FORMATO_DATA_HORA), motivo, telefone);
        enviarEmailNotificacao(destinatario, "NOWAIT — Agendamento recusado", "Agendamento recusado", mensagem);
    }

    public void enviarEmailAgendamentoCancelado(String destinatario, String estabelecimentoNome,
                                                 String telefone, LocalDateTime dataHora, String motivo) {
        String mensagem = String.format(
                "Seu agendamento no %s para %s foi cancelado pelo estabelecimento. Motivo informado: \"%s\". "
                        + "Qualquer dúvida, fale com o estabelecimento pelo telefone %s.",
                estabelecimentoNome, dataHora.format(FORMATO_DATA_HORA), motivo, telefone);
        enviarEmailNotificacao(destinatario, "NOWAIT — Agendamento cancelado", "Agendamento cancelado", mensagem);
    }

    public void enviarEmailAgendamentoCanceladoPeloCliente(String destinatario, String clienteNome, LocalDateTime dataHora) {
        String mensagem = String.format(
                "%s cancelou o agendamento para %s.", clienteNome, dataHora.format(FORMATO_DATA_HORA));
        enviarEmailNotificacao(destinatario, "NOWAIT — Agendamento cancelado pelo cliente", "Agendamento cancelado", mensagem);
    }

    public void enviarEmailFilaNovaEntrada(String destinatario, String clienteNome, int quantidadePessoas) {
        String mensagem = String.format(
                "%s entrou na fila de espera (%d pessoa(s)). Acesse o painel para chamar quando houver mesa disponível.",
                clienteNome, quantidadePessoas);
        enviarEmailNotificacao(destinatario, "NOWAIT — Novo cliente na fila", "Novo cliente na fila", mensagem);
    }

    public void enviarEmailFilaChamada(String destinatario, String estabelecimentoNome) {
        String mensagem = String.format(
                "Sua mesa no %s está pronta! Dirija-se ao estabelecimento.", estabelecimentoNome);
        enviarEmailNotificacao(destinatario, "NOWAIT — Você foi chamado!", "Você foi chamado!", mensagem);
    }

    public void enviarEmailFilaCanceladaPeloCliente(String destinatario, String clienteNome) {
        String mensagem = String.format("%s saiu da fila de espera.", clienteNome);
        enviarEmailNotificacao(destinatario, "NOWAIT — Cliente saiu da fila", "Cliente saiu da fila", mensagem);
    }

    public void enviarEmailFilaCanceladaPeloEstabelecimento(String destinatario, String estabelecimentoNome) {
        String mensagem = String.format("Sua posição na fila do %s foi cancelada pelo estabelecimento.", estabelecimentoNome);
        enviarEmailNotificacao(destinatario, "NOWAIT — Posição na fila cancelada", "Posição na fila cancelada", mensagem);
    }

    public void enviarComSeguranca(Runnable envio) {
        try {
            envio.run();
        } catch (RuntimeException e) {
            log.warn("Falha ao enviar e-mail de notificação: {}", e.getMessage());
        }
    }

    private void enviarEmailNotificacao(String destinatario, String assunto, String titulo, String mensagem) {
        String corpo = templateNotificacao.replace(PLACEHOLDER_TITULO, titulo).replace(PLACEHOLDER_MENSAGEM, mensagem);
        enviarEmail(destinatario, assunto, corpo);
        log.info("E-mail de notificação ('{}') enviado para: {}", assunto, destinatario);
    }

    private String construirLinkReset(String token) {
        return appUrl + "/reset-password?token=" + token;
    }

    private String construirLinkContestacaoBanimento(String token) {
        return appUrl + "/contestar-banimento?token=" + token;
    }

    private String construirLinkContestacaoRejeicaoEstabelecimento(String token) {
        return appUrl + "/contestar-rejeicao-estabelecimento?token=" + token;
    }

    private String carregarTemplate(String caminho) {
        try {
            byte[] bytes = new ClassPathResource(caminho).getInputStream().readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Falha ao carregar template '{}': {}", caminho, e.getMessage());
            throw new IllegalStateException("Template de e-mail não encontrado: " + caminho);
        }
    }

    private void enviarEmail(String destinatario, String assunto, String corpo) {
        try {
            MimeMessage mensagem = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensagem, true, "UTF-8");

            helper.setFrom(remetente, NOME_REMETENTE);
            helper.setTo(destinatario);
            helper.setSubject(assunto);
            helper.setText(corpo, true);

            mailSender.send(mensagem);
        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error("Falha ao enviar e-mail para {}: {}", destinatario, e.getMessage());
            throw new InvalidInputException("Não foi possível enviar o e-mail. Tente novamente.");
        }
    }
}
