package com.delivery.tracker.auth;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.security.autoconfigure.web.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods=false)
@ConditionalOnProperty(name="spring.h2.console.enabled",havingValue="true")
public class H2ConsoleSecurityConfig {
    @Bean @Order(0)
    SecurityFilterChain h2ConsoleSecurity(HttpSecurity http) throws Exception {
        http.securityMatcher(PathRequest.toH2Console())
            .authorizeHttpRequests(auth->auth.anyRequest().permitAll())
            .csrf(csrf->csrf.disable())
            .headers(headers->headers.frameOptions(frame->frame.sameOrigin()));
        return http.build();
    }
}
