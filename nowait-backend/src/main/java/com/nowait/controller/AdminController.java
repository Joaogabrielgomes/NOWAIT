package com.nowait.controller;

import com.nowait.dto.BanimentoDTO;
import com.nowait.dto.ContestacaoBanimentoResponseDTO;
import com.nowait.dto.ContestacaoRejeicaoEstabelecimentoResponseDTO;
import com.nowait.dto.ContestacaoResponseDTO;
import com.nowait.dto.DashboardResumoDTO;
import com.nowait.dto.DenunciaResolucaoDTO;
import com.nowait.dto.DenunciaResponseDTO;
import com.nowait.dto.EstabelecimentoResponseDTO;
import com.nowait.dto.LogAuditoriaResponseDTO;
import com.nowait.dto.RejeicaoEstabelecimentoDTO;
import com.nowait.dto.UsuarioResponseDTO;
import com.nowait.model.AcaoAuditoria;
import com.nowait.model.Usuario;
import com.nowait.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/usuarios")
    public ResponseEntity<Page<UsuarioResponseDTO>> listarUsuarios(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String nome,
            @RequestParam(defaultValue = "nome") String campo,
            @RequestParam(defaultValue = "asc") String ordem,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {
        return ResponseEntity.ok(adminService.listarUsuarios(role, nome, campo, ordem, pagina, tamanho));
    }

    @DeleteMapping("/usuarios/{id}")
    public ResponseEntity<Void> removerUsuario(@PathVariable String id, @AuthenticationPrincipal Usuario admin) {
        adminService.removerUsuario(id, admin);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/usuarios/{id}/banir")
    public ResponseEntity<UsuarioResponseDTO> banirUsuario(
            @PathVariable String id, @Valid @RequestBody BanimentoDTO dto, @AuthenticationPrincipal Usuario admin) {
        return ResponseEntity.ok(adminService.banirUsuario(id, dto.getMotivo(), admin));
    }

    @PatchMapping("/usuarios/{id}/desbanir")
    public ResponseEntity<UsuarioResponseDTO> desbanirUsuario(
            @PathVariable String id, @AuthenticationPrincipal Usuario admin) {
        return ResponseEntity.ok(adminService.desbanirUsuario(id, admin));
    }

    @GetMapping("/denuncias")
    public ResponseEntity<Page<DenunciaResponseDTO>> listarDenuncias(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String nome,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {
        return ResponseEntity.ok(adminService.listarDenuncias(status, nome, pagina, tamanho));
    }

    @PatchMapping("/denuncias/{id}/resolver")
    public ResponseEntity<DenunciaResponseDTO> resolverDenuncia(
            @PathVariable String id,
            @Valid @RequestBody DenunciaResolucaoDTO dto,
            @AuthenticationPrincipal Usuario admin) {
        return ResponseEntity.ok(adminService.resolverDenuncia(id, admin, dto));
    }

    @GetMapping("/contestacoes-banimento")
    public ResponseEntity<Page<ContestacaoBanimentoResponseDTO>> listarContestacoesBanimento(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String nome,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {
        return ResponseEntity.ok(adminService.listarContestacoesBanimento(status, nome, pagina, tamanho));
    }

    @PatchMapping("/contestacoes-banimento/{id}/analisar")
    public ResponseEntity<ContestacaoBanimentoResponseDTO> marcarContestacaoAnalisada(
            @PathVariable String id, @AuthenticationPrincipal Usuario admin) {
        return ResponseEntity.ok(adminService.marcarContestacaoAnalisada(id, admin));
    }

    @GetMapping("/contestacoes")
    public ResponseEntity<Page<ContestacaoResponseDTO>> listarContestacoes(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String nome,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {
        return ResponseEntity.ok(adminService.listarContestacoes(status, nome, pagina, tamanho));
    }

    @PatchMapping("/contestacoes-rejeicao-estabelecimento/{id}/analisar")
    public ResponseEntity<ContestacaoRejeicaoEstabelecimentoResponseDTO> marcarContestacaoRejeicaoEstabelecimentoAnalisada(
            @PathVariable String id, @AuthenticationPrincipal Usuario admin) {
        return ResponseEntity.ok(adminService.marcarContestacaoRejeicaoEstabelecimentoAnalisada(id, admin));
    }

    @GetMapping("/estabelecimentos")
    public ResponseEntity<Page<EstabelecimentoResponseDTO>> listarEstabelecimentos(
            @RequestParam(required = false) String statusAprovacao,
            @RequestParam(required = false) String nome,
            @RequestParam(defaultValue = "nome") String campo,
            @RequestParam(defaultValue = "asc") String ordem,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {
        return ResponseEntity.ok(adminService.listarEstabelecimentos(statusAprovacao, nome, campo, ordem, pagina, tamanho));
    }

    @PatchMapping("/estabelecimentos/{id}/aprovar")
    public ResponseEntity<EstabelecimentoResponseDTO> aprovarEstabelecimento(
            @PathVariable String id, @AuthenticationPrincipal Usuario admin) {
        return ResponseEntity.ok(adminService.aprovarEstabelecimento(id, admin));
    }

    @PatchMapping("/estabelecimentos/{id}/rejeitar")
    public ResponseEntity<EstabelecimentoResponseDTO> rejeitarEstabelecimento(
            @PathVariable String id,
            @Valid @RequestBody RejeicaoEstabelecimentoDTO dto,
            @AuthenticationPrincipal Usuario admin) {
        return ResponseEntity.ok(adminService.rejeitarEstabelecimento(id, dto.getMotivo(), admin));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResumoDTO> dashboard() {
        return ResponseEntity.ok(adminService.obterResumoDashboard());
    }

    @GetMapping("/auditoria")
    public ResponseEntity<Page<LogAuditoriaResponseDTO>> listarAuditoria(
            @RequestParam(required = false) String usuarioId,
            @RequestParam(required = false) AcaoAuditoria acao,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {
        return ResponseEntity.ok(adminService.listarAuditoria(usuarioId, acao, pagina, tamanho));
    }

    @GetMapping("/auditoria/export")
    public ResponseEntity<byte[]> exportarAuditoria(
            @RequestParam(required = false) String usuarioId,
            @RequestParam(required = false) AcaoAuditoria acao) {
        byte[] csv = adminService.exportarAuditoriaCsv(usuarioId, acao);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=auditoria.csv")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }
}
