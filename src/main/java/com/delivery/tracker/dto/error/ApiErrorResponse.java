package com.delivery.tracker.dto.error;


public record ApiErrorResponse(int status, String code, String message) {}
