package com.delivery.tracker.exception;

import com.delivery.tracker.dto.error.ApiErrorResponse;

import java.io.IOException;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.dao.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.security.access.AccessDeniedException;

@RestControllerAdvice
public class ApiErrors {
    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiErrorResponse> business(ApiException ex) { return reply(ex.status(),ex.code(),ex.getMessage()); }
    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ApiErrorResponse> invalid(Exception ex) { return reply(400,"INVALID_INPUT","Dados inválidos"); }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiErrorResponse> integrity(Exception ex) { return reply(409,"CONFLICT","Conflito com dados existentes"); }
    @ExceptionHandler({OptimisticLockingFailureException.class, PessimisticLockingFailureException.class})
    ResponseEntity<ApiErrorResponse> concurrent(Exception ex) { return reply(409,"CONFLICT","Dados alterados por outra operação; consulte novamente"); }
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiErrorResponse> forbidden(Exception ex) { return reply(403,"FORBIDDEN","Operação não permitida"); }
    private ResponseEntity<ApiErrorResponse> reply(int status,String code,String message) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(status,code,message));
    }
    public static void security(HttpServletResponse response,int status) throws IOException {
        response.setStatus(status); response.setContentType("application/json;charset=UTF-8");
        var code = status == 401 ? "UNAUTHORIZED" : "FORBIDDEN";
        var message = status == 401 ? "Autenticação necessária ou inválida" : "Operação não permitida";
        response.getWriter().write("{\"status\":"+status+",\"code\":\""+code+"\",\"message\":\""+message+"\"}");
    }
}
