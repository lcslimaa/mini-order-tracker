package com.delivery.tracker.auth;

import com.delivery.tracker.common.ApiException;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OperatorBootstrap implements ApplicationRunner {
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final Validator validator;
    private final String name,email,password;
    public OperatorBootstrap(UserRepository users,PasswordEncoder passwords,Validator validator,
        @Value("${app.operator.name}") String name,@Value("${app.operator.email}") String email,@Value("${app.operator.password}") String password) {
        this.users=users;this.passwords=passwords;this.validator=validator;this.name=name;this.email=email;this.password=password;
    }
    @Override @Transactional public void run(ApplicationArguments args) {
        if(name.isBlank() && email.isBlank() && password.isBlank()) return;
        var input=new AuthController.Register(name,email,password);
        if(!validator.validate(input).isEmpty()) throw new IllegalStateException("Configuração inicial do operador inválida ou incompleta");
        try { AuthService.validatePassword(password); } catch(ApiException ex) { throw new IllegalStateException("Senha inicial do operador inválida"); }
        var existing=users.findByEmail(input.email());
        if(existing.isPresent()) {
            if(existing.get().getRole()!=Role.OPERADOR) throw new IllegalStateException("Email inicial do operador já pertence a CLIENTE");
            return;
        }
        users.saveAndFlush(new AppUser(name.strip(),input.email(),passwords.encode(password),Role.OPERADOR));
    }
}
