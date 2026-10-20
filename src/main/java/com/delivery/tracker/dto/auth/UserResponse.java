package com.delivery.tracker.dto.auth;

import com.delivery.tracker.enums.Role;


public record UserResponse(Long id,String name,String email,Role role) {}
