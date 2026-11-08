package com.nowait.controller;

import com.nowait.dto.DashboardResumoDTO;
import com.nowait.dto.EstabelecimentoResponseDTO;
import com.nowait.dto.LogAuditoriaResponseDTO;
import com.nowait.dto.UsuarioResponseDTO;
import com.nowait.model.AcaoAuditoria;
import com.nowait.model.Usuario;
import com.nowait.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/usuarios")
    public ResponseEntity<List<UsuarioResponseDTO>> listarUsuarios() {
        return ResponseEntity.ok(adminService.listarUsuarios());
    }

    @DeleteMapping("/usuarios/{id}")
    public ResponseEntity<Void> removerUsuario(@PathVariable String id, @AuthenticationPrincipal Usuario admin) {
        adminService.removerUsuario(id, admin);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/estabelecimentos")
    public ResponseEntity<List<EstabelecimentoResponseDTO>> listarEstabelecimentos() {
        return ResponseEntity.ok(adminService.listarEstabelecimentos());
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
