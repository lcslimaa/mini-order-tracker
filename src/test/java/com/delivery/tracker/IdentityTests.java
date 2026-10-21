package com.delivery.tracker;

import java.util.Map;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class IdentityTests extends ApiSupport {
    @Autowired JwtEncoder encoder;
    @Autowired com.delivery.tracker.repository.UserRepository users;

    @Test void returnsOnlyOwnPublicIdentityForEachRole() throws Exception {
        var token=client("identity@example.com");
        var result=request("GET","/auth/me?id=1&role=OPERADOR",token,null);
        assertThat(result.status()).isEqualTo(200);
        assertThat(result.body().path("email").asText()).isEqualTo("identity@example.com");
        assertThat(result.body().path("role").asText()).isEqualTo("CLIENTE");
        assertThat(result.body().path("id").asLong()).isEqualTo(users.findByEmail("identity@example.com").orElseThrow().getId());
        assertThat(result.body().properties()).extracting(Map.Entry::getKey).containsExactlyInAnyOrder("id","name","email","role");
        var operator=request("GET","/auth/me",login("operator@example.com","operator-password"),null);
        assertThat(operator.status()).isEqualTo(200);
        assertThat(operator.body().path("role").asText()).isEqualTo("OPERADOR");
    }

    @Test void rejectsMissingMalformedExpiredAndDeletedIdentities() throws Exception {
        assertThat(request("GET","/auth/me",null,null).status()).isEqualTo(401);
        assertThat(request("GET","/auth/me","invalid",null).status()).isEqualTo(401);
        var header=JwsHeader.with(MacAlgorithm.HS256).build();
        for (var claims : new JwtClaimsSet[]{
            JwtClaimsSet.builder().issuer("mini-order-tracker").subject("1").issuedAt(Instant.now().minusSeconds(100)).expiresAt(Instant.now().minusSeconds(1)).build(),
            JwtClaimsSet.builder().issuer("mini-order-tracker").subject("99999999").issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build()
        }) {
            var token=encoder.encode(JwtEncoderParameters.from(header,claims)).getTokenValue();
            assertThat(request("GET","/auth/me",token,null).status()).isEqualTo(401);
        }
    }
}
