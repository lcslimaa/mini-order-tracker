package com.delivery.tracker.controller;

import com.delivery.tracker.dto.auth.LoginRequest;
import com.delivery.tracker.dto.auth.RegisterRequest;
import com.delivery.tracker.dto.auth.TokenResponse;
import com.delivery.tracker.dto.auth.UserResponse;
import com.delivery.tracker.service.AuthService;

import jakarta.validation.Valid;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;
    private final JwtEncoder encoder;
    private final long ttl;
    public AuthController(AuthService auth,JwtEncoder encoder,@Value("${app.jwt.ttl-seconds}") long ttl) {
        if (ttl <= 0) throw new IllegalArgumentException("JWT_TTL_SECONDS deve ser positivo");
        this.auth=auth;this.encoder=encoder;this.ttl=ttl;
    }
    @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED)
    UserResponse register(@Valid @RequestBody RegisterRequest request) {
        var u=auth.register(request.name(),request.email(),request.password());
        return new UserResponse(u.getId(),u.getName(),u.getEmail(),u.getRole());
    }
    @PostMapping("/login")
    TokenResponse login(@Valid @RequestBody LoginRequest request) {
        var u=auth.login(request.email(),request.password()); var now=Instant.now();
        var claims=JwtClaimsSet.builder().issuer("mini-order-tracker").subject(u.getId().toString()).issuedAt(now).expiresAt(now.plusSeconds(ttl)).build();
        var header=JwsHeader.with(MacAlgorithm.HS256).build();
        return new TokenResponse(encoder.encode(JwtEncoderParameters.from(header,claims)).getTokenValue(),"Bearer",ttl);
    }
}
