package com.delivery.tracker.common;

public class ApiException extends RuntimeException {
    private final int status;
    private final String code;
    public ApiException(int status, String code, String message) {
        super(message); this.status = status; this.code = code;
    }
    public int status() { return status; }
    public String code() { return code; }
    public static ApiException notFound() { return new ApiException(404,"NOT_FOUND","Recurso não encontrado"); }
    public static ApiException invalid() { return new ApiException(400,"INVALID_INPUT","Dados inválidos"); }
    public static ApiException conflict() { return new ApiException(409,"CONFLICT","Operação incompatível com o estado atual"); }
}
