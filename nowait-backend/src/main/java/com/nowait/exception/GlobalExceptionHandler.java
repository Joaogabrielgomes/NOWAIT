package com.nowait.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> camposComErro = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        FieldError::getDefaultMessage,
                        (mensagemExistente, novaMensagem) -> mensagemExistente));
        return buildResponse(HttpStatus.BAD_REQUEST, "Dados inválidos", "Verifique os campos indicados", camposComErro);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponseDTO> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> camposComErro = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        violacao -> violacao.getPropertyPath().toString(),
                        violacao -> violacao.getMessage(),
                        (mensagemExistente, novaMensagem) -> mensagemExistente));
        return buildResponse(HttpStatus.BAD_REQUEST, "Parâmetro inválido", "Verifique os parâmetros indicados", camposComErro);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleResourceNotFound(ResourceNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "Recurso não encontrado", ex.getMessage());
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponseDTO> handleDuplicateResource(DuplicateResourceException ex) {
        return buildResponse(HttpStatus.CONFLICT, "Recurso duplicado", ex.getMessage());
    }

    @ExceptionHandler(UnauthorizedOperationException.class)
    public ResponseEntity<ErrorResponseDTO> handleUnauthorizedOperation(UnauthorizedOperationException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, "Operação não permitida", ex.getMessage());
    }

    @ExceptionHandler(InvalidInputException.class)
    public ResponseEntity<ErrorResponseDTO> handleInvalidInput(InvalidInputException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Entrada inválida", ex.getMessage());
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponseDTO> handleBusinessRule(BusinessRuleException ex) {
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, "Regra de negócio violada", ex.getMessage());
    }

    @ExceptionHandler(CepNaoEncontradoException.class)
    public ResponseEntity<ErrorResponseDTO> handleCepNaoEncontrado(CepNaoEncontradoException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "CEP não encontrado", ex.getMessage());
    }

    @ExceptionHandler(CepServiceIndisponivelException.class)
    public ResponseEntity<ErrorResponseDTO> handleCepIndisponivel(CepServiceIndisponivelException ex) {
        log.error("Falha ao consultar serviço de CEP: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, "Serviço de CEP indisponível",
                "Não foi possível consultar o CEP no momento. Tente novamente mais tarde.");
    }

    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<ErrorResponseDTO> handleResourceAccess(ResourceAccessException ex) {
        log.error("Falha de acesso a recurso externo: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, "Serviço externo indisponível",
                "Não foi possível se comunicar com um serviço externo. Tente novamente mais tarde.");
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponseDTO> handleBadCredentials(BadCredentialsException ex) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "Não autorizado", "E-mail ou senha incorretos");
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleUsernameNotFound(UsernameNotFoundException ex) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "Não autorizado", ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDTO> handleAccessDenied(AccessDeniedException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, "Acesso negado",
                "Você não tem permissão para acessar este recurso");
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponseDTO> handleRuntimeException(RuntimeException ex) {
        log.error("RuntimeException não tratada: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno",
                "Ocorreu um erro inesperado. Tente novamente mais tarde.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGenericException(Exception ex) {
        log.error("Exception não tratada: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno",
                "Ocorreu um erro inesperado. Tente novamente mais tarde.");
    }

    private ResponseEntity<ErrorResponseDTO> buildResponse(HttpStatus status, String error, String message) {
        return buildResponse(status, error, message, null);
    }

    private ResponseEntity<ErrorResponseDTO> buildResponse(HttpStatus status, String error, String message,
                                                             Map<String, String> camposComErro) {
        ErrorResponseDTO body = ErrorResponseDTO.builder()
                .status(status.value())
                .error(error)
                .message(message)
                .camposComErro(camposComErro)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(status).body(body);
    }
}
