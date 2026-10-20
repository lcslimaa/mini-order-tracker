package com.delivery.tracker.security;

import com.delivery.tracker.enums.Role;
public record Identity(Long id, Role role) {
    public boolean operator() { return role == Role.OPERADOR; }
}
