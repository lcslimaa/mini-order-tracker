package com.delivery.tracker.config;

import com.delivery.tracker.exception.ApiErrors;
import com.delivery.tracker.repository.UserRepository;
import com.delivery.tracker.security.Identity;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import javax.crypto.spec.SecretKeySpec;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;
import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean SecretKeySpec jwtKey(@Value("${app.jwt.secret}") String secret) {
        byte[] bytes=secret.getBytes(StandardCharsets.UTF_8);
        if(bytes.length < 32) throw new IllegalArgumentException("JWT_SECRET precisa de ao menos 32 bytes");
        return new SecretKeySpec(bytes,"HmacSHA256");
    }
    @Bean JwtEncoder jwtEncoder(SecretKeySpec key) { return new NimbusJwtEncoder(new ImmutableSecret<>(key)); }
    @Bean JwtDecoder jwtDecoder(SecretKeySpec key) {
        var decoder=NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(new JwtTimestampValidator(Duration.ZERO),new JwtIssuerValidator("mini-order-tracker"),
            jwt -> jwt.getExpiresAt() == null || jwt.getSubject() == null
                ? OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token")) : OAuth2TokenValidatorResult.success()));
        return decoder;
    }
    @Bean SecurityFilterChain security(HttpSecurity http,UserRepository users) throws Exception {
        http.csrf(csrf->csrf.disable()).sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a->a
                .requestMatchers(HttpMethod.POST,"/api/auth/register","/api/auth/login").permitAll()
                .requestMatchers(HttpMethod.POST,"/api/products").hasRole("OPERADOR")
                .requestMatchers(HttpMethod.PUT,"/api/products/*").hasRole("OPERADOR")
                .requestMatchers(HttpMethod.PATCH,"/api/orders/*/status").hasRole("OPERADOR")
                .requestMatchers(HttpMethod.POST,"/api/orders").hasRole("CLIENTE")
                .anyRequest().authenticated())
            .exceptionHandling(e->e.authenticationEntryPoint((req,res,ex)->ApiErrors.security(res,401)).accessDeniedHandler((req,res,ex)->ApiErrors.security(res,403)))
            .oauth2ResourceServer(o->o
                .authenticationEntryPoint((req,res,ex)->ApiErrors.security(res,401))
                .accessDeniedHandler((req,res,ex)->ApiErrors.security(res,403))
                .jwt(j->j.jwtAuthenticationConverter(jwt->{
                    Long id;
                    try { id=Long.valueOf(jwt.getSubject()); } catch (NumberFormatException ex) { throw new BadCredentialsException("Token inválido"); }
                    var u=users.findById(id).orElseThrow(()->new BadCredentialsException("Token inválido"));
                    return new UsernamePasswordAuthenticationToken(new Identity(u.getId(),u.getRole()),null,List.of(new SimpleGrantedAuthority("ROLE_"+u.getRole())));
                })));
        return http.build();
    }
}
