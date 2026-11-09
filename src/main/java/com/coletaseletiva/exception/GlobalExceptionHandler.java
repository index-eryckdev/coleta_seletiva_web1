package com.coletaseletiva.exception;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

/**
 * Tratamento global de erros (RNF03).
 * 400 entrada inválida | 404 não encontrado | 409 conflito | 500 erro inesperado.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ---------- 400 ----------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validacao(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<CampoErro> erros = new ArrayList<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> erros.add(new CampoErro(fe.getField(), fe.getDefaultMessage())));
        ex.getBindingResult().getGlobalErrors()
                .forEach(ge -> erros.add(new CampoErro(ge.getObjectName(), ge.getDefaultMessage())));
        return montar(HttpStatus.BAD_REQUEST, "VALIDACAO", "Dados inválidos.", req, erros);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> violacoes(ConstraintViolationException ex, HttpServletRequest req) {
        List<CampoErro> erros = ex.getConstraintViolations().stream()
                .map(v -> new CampoErro(v.getPropertyPath().toString(), v.getMessage()))
                .toList();
        return montar(HttpStatus.BAD_REQUEST, "VALIDACAO", "Dados inválidos.", req, erros);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> corpoInvalido(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return montar(HttpStatus.BAD_REQUEST, "CORPO_INVALIDO",
                "Corpo da requisição ausente ou malformado.", req, List.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> tipoInvalido(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        List<CampoErro> erros = List.of(new CampoErro(ex.getName(), "Valor inválido."));
        return montar(HttpStatus.BAD_REQUEST, "PARAMETRO_INVALIDO", "Parâmetro inválido.", req, erros);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> parametroAusente(MissingServletRequestParameterException ex,
            HttpServletRequest req) {
        List<CampoErro> erros = List.of(new CampoErro(ex.getParameterName(), "Parâmetro obrigatório."));
        return montar(HttpStatus.BAD_REQUEST, "PARAMETRO_AUSENTE", "Parâmetro obrigatório ausente.", req, erros);
    }

    // ---------- 404 ----------

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ApiError> naoEncontrado(RecursoNaoEncontradoException ex, HttpServletRequest req) {
        return montar(HttpStatus.NOT_FOUND, "NAO_ENCONTRADO", ex.getMessage(), req, List.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> rotaInexistente(NoResourceFoundException ex, HttpServletRequest req) {
        return montar(HttpStatus.NOT_FOUND, "NAO_ENCONTRADO", "Recurso não encontrado.", req, List.of());
    }

    // ---------- 405 ----------

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> metodoNaoSuportado(HttpRequestMethodNotSupportedException ex,
            HttpServletRequest req) {
        return montar(HttpStatus.METHOD_NOT_ALLOWED, "METODO_NAO_SUPORTADO",
                "Método HTTP não suportado para este recurso.", req, List.of());
    }

    // ---------- 409 ----------

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<ApiError> regraNegocio(RegraNegocioException ex, HttpServletRequest req) {
        return montar(HttpStatus.CONFLICT, "REGRA_NEGOCIO", ex.getMessage(), req, List.of());
    }

    @ExceptionHandler({ ObjectOptimisticLockingFailureException.class, OptimisticLockException.class })
    public ResponseEntity<ApiError> conflitoVersao(Exception ex, HttpServletRequest req) {
        return montar(HttpStatus.CONFLICT, "CONFLITO_VERSAO",
                "O registro foi alterado por outra operação. Recarregue e tente novamente.", req, List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> integridade(DataIntegrityViolationException ex, HttpServletRequest req) {
        log.warn("Violação de integridade em {}: {}", req.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return montar(HttpStatus.CONFLICT, "CONFLITO_DADOS",
                "A operação viola uma restrição de dados (duplicidade ou vínculo existente).", req, List.of());
    }

    // ---------- 500 ----------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> inesperado(Exception ex, HttpServletRequest req) {
        log.error("Erro inesperado em {}", req.getRequestURI(), ex); // stack trace só no log do servidor
        return montar(HttpStatus.INTERNAL_SERVER_ERROR, "ERRO_INTERNO",
                "Erro interno. Tente novamente mais tarde.", req, List.of());
    }

    private ResponseEntity<ApiError> montar(HttpStatus status, String codigo, String mensagem,
            HttpServletRequest req, List<CampoErro> erros) {
        ApiError corpo = new ApiError(OffsetDateTime.now(), status.value(), codigo, mensagem,
                req.getRequestURI(), erros);
        return ResponseEntity.status(status).body(corpo);
    }
}
