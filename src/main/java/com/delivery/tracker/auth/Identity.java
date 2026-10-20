package com.delivery.tracker.auth;
public record Identity(Long id, Role role) {
    public boolean operator() { return role == Role.OPERADOR; }
}
