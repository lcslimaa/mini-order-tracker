package com.delivery.tracker.service;

import com.delivery.tracker.entity.AppUser;
import com.delivery.tracker.enums.Role;
import com.delivery.tracker.exception.ApiException;
import com.delivery.tracker.repository.UserRepository;
import com.delivery.tracker.util.EmailNormalizer;

import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final String dummyHash;
    public AuthService(UserRepository users,PasswordEncoder passwords) {
        this.users=users; this.passwords=passwords; this.dummyHash=passwords.encode("dummy-login-password");
    }
    @Transactional(readOnly=true)
    public com.delivery.tracker.dto.auth.UserResponse current(Long id) {
        var user=users.findById(id).orElseThrow(()->new ApiException(401,"UNAUTHORIZED","Autenticação necessária ou inválida"));
        return new com.delivery.tracker.dto.auth.UserResponse(user.getId(),user.getName(),user.getEmail(),user.getRole());
    }
    public static void validatePassword(String password) {
        if (password == null || password.isBlank() || password.getBytes(StandardCharsets.UTF_8).length > 72) throw ApiException.invalid();
    }
    @Transactional
    public AppUser register(String name,String email,String password) {
        validatePassword(password);
        return users.saveAndFlush(new AppUser(name.strip(),EmailNormalizer.normalize(email),passwords.encode(password),Role.CLIENTE));
    }
    @Transactional(readOnly=true)
    public AppUser login(String email,String password) {
        if(password == null || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ApiException(401,"UNAUTHORIZED","Email ou senha inválidos");
        }
        var user=users.findByEmail(EmailNormalizer.normalize(email));
        // Perform BCrypt even for unknown emails to reduce account enumeration by timing.
        boolean valid = passwords.matches(password == null ? "" : password,user.map(AppUser::getPasswordHash).orElse(dummyHash));
        if (!valid || user.isEmpty()) throw new ApiException(401,"UNAUTHORIZED","Email ou senha inválidos");
        return user.get();
    }
}
