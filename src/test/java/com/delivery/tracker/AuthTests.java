package com.delivery.tracker;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AuthTests extends ApiSupport {
    @Test void registersClientAndRejectsDuplicateAndPrivilegedFields() throws Exception {
        var result = request("POST", "/auth/register", null, Map.of("name","Ana","email"," Ana@Example.com ","password","test-password"));
        assertThat(result.status()).isEqualTo(201);
        assertThat(result.body().path("email").asText()).isEqualTo("ana@example.com");
        assertThat(result.body().path("role").asText()).isEqualTo("CLIENTE");
        assertThat(result.body().has("password")).isFalse();
        assertThat(result.body().has("passwordHash")).isFalse();
        assertThat(request("POST","/auth/register",null,Map.of("name","Outro","email","ANA@example.com","password","x")).status()).isEqualTo(409);
        assertThat(request("POST","/auth/register",null,Map.of("name","Outro","email","other@example.com","password","x","role","OPERADOR")).status()).isEqualTo(400);
        var token = login("ana@example.com", "test-password");
        assertThat(token).isNotBlank();
    }
    @Test void rejectsInvalidCredentialsAndAnonymousRequests() throws Exception {
        assertThat(request("POST","/auth/login",null,Map.of("email","missing@example.com","password","wrong")).status()).isEqualTo(401);
        for (var route : new String[][]{{"GET","/products"},{"POST","/products"},{"PUT","/products/1"},{"GET","/orders"},{"POST","/orders"},{"GET","/orders/1"},{"PATCH","/orders/1/status"},{"POST","/orders/1/cancel"}}) {
            var result = request(route[0],route[1],null, route[0].equals("GET") ? null : Map.of());
            assertThat(result.status()).as(route[0]+route[1]).isEqualTo(401);
            assertThat(result.body().path("code").asText()).isEqualTo("UNAUTHORIZED");
        }
    }

    @org.springframework.beans.factory.annotation.Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    @Test void hashesPasswordsAndValidatesRegistration() throws Exception {
        client("hash@example.com");
        var hash = jdbc.queryForObject("select password_hash from app_users where email='hash@example.com'",String.class);
        assertThat(hash).startsWith("$2");
        assertThat(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().matches("client-password",hash)).isTrue();
        for (var body : new Object[]{Map.of("name","","email","ok@example.com","password","x"), Map.of("name","A","email","bad","password","x"), Map.of("name","A","email","ok@example.com","password",""), Map.of("name","A","email","ok@example.com","password","á".repeat(37))}) {
            assertThat(request("POST","/auth/register",null,body).status()).isEqualTo(400);
        }
    }
    @Test void concurrentDuplicateRegistrationsCreateOneUser() throws Exception {
        try (var pool = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            var barrier = new java.util.concurrent.CyclicBarrier(2);
            var jobs = java.util.stream.IntStream.range(0,2).mapToObj(n -> pool.submit(() -> {
                barrier.await();
                return request("POST","/auth/register",null,Map.of("name","A","email","race@example.com","password","x")).status();
            })).toList();
            assertThat(java.util.List.of(jobs.get(0).get(),jobs.get(1).get())).containsExactlyInAnyOrder(201,409);
        }
        assertThat(jdbc.queryForObject("select count(*) from app_users where email='race@example.com'",Long.class)).isEqualTo(1);
    }
    @Test void operatorLoginAndClientRoleRestrictions() throws Exception {
        var operator = login("operator@example.com","operator-password");
        assertThat(operator).isNotBlank();
        var token = client("role@example.com");
        assertThat(request("POST","/products",token,Map.of()).status()).isEqualTo(403);
        assertThat(request("PUT","/products/1",token,Map.of()).status()).isEqualTo(403);
        assertThat(request("PATCH","/orders/1/status",token,Map.of()).status()).isEqualTo(403);
        assertThat(request("POST","/orders",operator,Map.of()).status()).isEqualTo(403);
    }
    @Test void invalidJwtNeverAccessesProtectedRoutes() throws Exception {
        var token = client("jwt@example.com");
        var pieces = token.split("\\.");
        var damaged = pieces[0]+"."+pieces[1]+"."+(pieces[2].charAt(0)=='A'?"B":"A")+pieces[2].substring(1);
        assertThat(request("GET","/orders",damaged,null).status()).isEqualTo(401);
        assertThat(request("GET","/orders","not-a-jwt",null).status()).isEqualTo(401);
        var key = new javax.crypto.spec.SecretKeySpec("different-secret-012345678901234567890123456789".getBytes(java.nio.charset.StandardCharsets.UTF_8),"HmacSHA256");
        var encoder = new org.springframework.security.oauth2.jwt.NimbusJwtEncoder(new com.nimbusds.jose.jwk.source.ImmutableSecret<>(key));
        var claims = org.springframework.security.oauth2.jwt.JwtClaimsSet.builder().issuer("mini-order-tracker").subject("1").issuedAt(java.time.Instant.now()).expiresAt(java.time.Instant.now().plusSeconds(600)).build();
        var header = org.springframework.security.oauth2.jwt.JwsHeader.with(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256).build();
        var wrongKey = encoder.encode(org.springframework.security.oauth2.jwt.JwtEncoderParameters.from(header,claims)).getTokenValue();
        assertThat(request("GET","/orders",wrongKey,null).status()).isEqualTo(401);
    }

    @org.springframework.beans.factory.annotation.Autowired org.springframework.security.oauth2.jwt.JwtEncoder encoder;
    @org.springframework.beans.factory.annotation.Autowired com.delivery.tracker.auth.UserRepository users;
    @org.springframework.beans.factory.annotation.Autowired org.springframework.security.crypto.password.PasswordEncoder passwords;
    @org.springframework.beans.factory.annotation.Autowired jakarta.validation.Validator validator;
    @Test void expiredOrNonExpiringTokenIsRejected() throws Exception {
        client("expired@example.com");
        var id=users.findByEmail("expired@example.com").orElseThrow().getId().toString();
        var header=org.springframework.security.oauth2.jwt.JwsHeader.with(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256).build();
        var claims=org.springframework.security.oauth2.jwt.JwtClaimsSet.builder().issuer("mini-order-tracker").subject(id).issuedAt(java.time.Instant.now().minusSeconds(100));
        var expired=encoder.encode(org.springframework.security.oauth2.jwt.JwtEncoderParameters.from(header,claims.expiresAt(java.time.Instant.now().minusSeconds(1)).build())).getTokenValue();
        assertThat(request("GET","/orders",expired,null).status()).isEqualTo(401);
        var noExpiry=encoder.encode(org.springframework.security.oauth2.jwt.JwtEncoderParameters.from(header,org.springframework.security.oauth2.jwt.JwtClaimsSet.builder().issuer("mini-order-tracker").subject(id).build())).getTokenValue();
        assertThat(request("GET","/orders",noExpiry,null).status()).isEqualTo(401);
    }
    @Test void bootstrapPreservesOperatorAndRejectsClientCollisionAndPartialConfiguration() throws Exception {
        var args=new org.springframework.boot.DefaultApplicationArguments();
        var before=users.findByEmail("operator@example.com").orElseThrow().getPasswordHash();
        new com.delivery.tracker.auth.OperatorBootstrap(users,passwords,validator,"Novo Nome","operator@example.com","changed-password").run(args);
        assertThat(users.findByEmail("operator@example.com").orElseThrow().getPasswordHash()).isEqualTo(before);
        client("bootstrap-client@example.com");
        org.assertj.core.api.Assertions.assertThatThrownBy(()->new com.delivery.tracker.auth.OperatorBootstrap(users,passwords,validator,"Operador","bootstrap-client@example.com","x").run(args)).isInstanceOf(IllegalStateException.class);
        assertThat(users.findByEmail("bootstrap-client@example.com").orElseThrow().getRole()).isEqualTo(com.delivery.tracker.auth.Role.CLIENTE);
        org.assertj.core.api.Assertions.assertThatThrownBy(()->new com.delivery.tracker.auth.OperatorBootstrap(users,passwords,validator,"Operador","","").run(args)).isInstanceOf(IllegalStateException.class);
    }
}
