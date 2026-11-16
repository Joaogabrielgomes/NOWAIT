package com.nowait.controller;

import com.nowait.dto.ForgotPasswordDTO;
import com.nowait.dto.LoginRequestDTO;
import com.nowait.dto.LoginResponseDTO;
import com.nowait.dto.ResetPasswordDTO;
import com.nowait.dto.UsuarioRegistroDTO;
import com.nowait.service.AuthService;
import com.nowait.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO dto) {
        return ResponseEntity.ok(authService.login(dto));
    }

    @PostMapping("/register")
    public ResponseEntity<LoginResponseDTO> register(@Valid @RequestBody UsuarioRegistroDTO dto) {
        return ResponseEntity.status(201).body(authService.register(dto));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> esqueciSenha(@Valid @RequestBody ForgotPasswordDTO dto) {
        passwordResetService.solicitarResetSenha(dto.getEmail());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> redefinirSenha(@Valid @RequestBody ResetPasswordDTO dto) {
        passwordResetService.redefinirSenha(dto);
        return ResponseEntity.ok().build();
    }
}
