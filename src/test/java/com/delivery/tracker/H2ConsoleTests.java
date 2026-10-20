package com.delivery.tracker;

import java.net.URI;
import java.net.http.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties="spring.h2.console.enabled=true")
@ActiveProfiles("test")
class H2ConsoleTests {
    @LocalServerPort int port;
    @Test void enabledConsoleRendersLoginWhileApiRemainsProtected() throws Exception {
        try(var client=HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build()) {
            var response=client.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/h2-console/")).GET().build(),HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).contains("H2 Console");
            var redirect=java.util.regex.Pattern.compile("location.href = '([^']+)'").matcher(response.body());
            assertThat(redirect.find()).isTrue();
            var login=client.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/h2-console/"+redirect.group(1))).GET().build(),HttpResponse.BodyHandlers.ofString());
            assertThat(login.statusCode()).isEqualTo(200);
            assertThat(login.body()).contains("JDBC URL");
            assertThat(response.headers().firstValue("X-Frame-Options")).contains("SAMEORIGIN");
            var api=client.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/api/orders")).GET().build(),HttpResponse.BodyHandlers.ofString());
            assertThat(api.statusCode()).isEqualTo(401);
            assertThat(api.headers().firstValue("X-Frame-Options")).contains("DENY");
        }
    }
}
