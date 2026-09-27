package com.nowait.service;

import com.nowait.model.AcaoAuditoria;
import com.nowait.model.LogAuditoria;
import com.nowait.model.Usuario;
import com.nowait.repository.LogAuditoriaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuditoriaService {

    private static final int RETENCAO_MESES = 12;

    private final LogAuditoriaRepository logAuditoriaRepository;

    public void registrar(Usuario usuario, AcaoAuditoria acao, String entidadeTipo, String entidadeId, String detalhes) {
        registrar(usuario != null ? usuario.getId() : null, acao, entidadeTipo, entidadeId, detalhes);
    }

    public void registrar(String usuarioId, AcaoAuditoria acao, String entidadeTipo, String entidadeId, String detalhes) {
        try {
            LogAuditoria registro = LogAuditoria.builder()
                    .usuarioId(usuarioId)
                    .acao(acao)
                    .entidadeTipo(entidadeTipo)
                    .entidadeId(entidadeId)
                    .detalhes(detalhes)
                    .ip(extrairIpRequisicaoAtual())
                    .build();
            logAuditoriaRepository.save(registro);
        } catch (Exception e) {
            log.error("Falha ao registrar log de auditoria (acao={}): {}", acao, e.getMessage(), e);
        }
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void limparLogsAntigos() {
        LocalDateTime limite = LocalDateTime.now().minusMonths(RETENCAO_MESES);
        long removidos = logAuditoriaRepository.deleteByCriadoEmBefore(limite);
        if (removidos > 0) {
            log.info("Auditoria: {} log(s) com mais de {} meses removido(s)", removidos, RETENCAO_MESES);
        }
    }

    private String extrairIpRequisicaoAtual() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        HttpServletRequest request = attrs.getRequest();
        String encaminhadoPor = request.getHeader("X-Forwarded-For");
        if (encaminhadoPor != null && !encaminhadoPor.isBlank()) {
            return encaminhadoPor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
