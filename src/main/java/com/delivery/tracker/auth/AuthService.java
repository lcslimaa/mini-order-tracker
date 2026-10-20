package com.delivery.tracker.auth;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import com.delivery.tracker.common.ApiException;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final String dummyHash;
    public AuthService(UserRepository users,PasswordEncoder passwords) {
        this.users=users; this.passwords=passwords; this.dummyHash=passwords.encode("dummy-login-password");
    }
    public static String normalize(String email) { return email == null ? "" : email.strip().toLowerCase(Locale.ROOT); }
    public static void validatePassword(String password) {
        if (password == null || password.isBlank() || password.getBytes(StandardCharsets.UTF_8).length > 72) throw ApiException.invalid();
    }
    @Transactional
    public AppUser register(String name,String email,String password) {
        validatePassword(password);
        return users.saveAndFlush(new AppUser(name.strip(),normalize(email),passwords.encode(password),Role.CLIENTE));
    }
    @Transactional(readOnly=true)
    public AppUser login(String email,String password) {
        var user=users.findByEmail(normalize(email));
        // Perform BCrypt even for unknown emails to reduce account enumeration by timing.
        boolean valid = passwords.matches(password == null ? "" : password,user.map(AppUser::getPasswordHash).orElse(dummyHash));
        if (!valid || user.isEmpty()) throw new ApiException(401,"UNAUTHORIZED","Email ou senha inválidos");
        return user.get();
    }
}
