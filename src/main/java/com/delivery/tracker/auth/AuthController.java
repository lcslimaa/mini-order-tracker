package com.delivery.tracker.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    public record Register(@NotBlank @Size(max=160) String name, @NotBlank @Email @Size(max=254) String email, @NotBlank String password) {
        public Register { email=AuthService.normalize(email); }
    }
    public record Login(@NotBlank String email,@NotBlank String password) {}
    public record UserView(Long id,String name,String email,Role role) {}
    public record TokenView(String accessToken,String tokenType,long expiresIn) {}
    private final AuthService auth;
    private final JwtEncoder encoder;
    private final long ttl;
    public AuthController(AuthService auth,JwtEncoder encoder,@Value("${app.jwt.ttl-seconds}") long ttl) {
        if (ttl <= 0) throw new IllegalArgumentException("JWT_TTL_SECONDS deve ser positivo");
        this.auth=auth;this.encoder=encoder;this.ttl=ttl;
    }
    @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED)
    UserView register(@Valid @RequestBody Register request) {
        var u=auth.register(request.name(),request.email(),request.password());
        return new UserView(u.getId(),u.getName(),u.getEmail(),u.getRole());
    }
    @PostMapping("/login")
    TokenView login(@Valid @RequestBody Login request) {
        var u=auth.login(request.email(),request.password()); var now=Instant.now();
        var claims=JwtClaimsSet.builder().issuer("mini-order-tracker").subject(u.getId().toString()).issuedAt(now).expiresAt(now.plusSeconds(ttl)).build();
        var header=JwsHeader.with(MacAlgorithm.HS256).build();
        return new TokenView(encoder.encode(JwtEncoderParameters.from(header,claims)).getTokenValue(),"Bearer",ttl);
    }
}
