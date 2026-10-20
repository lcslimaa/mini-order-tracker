package com.delivery.tracker;

import java.net.URI;
import java.net.http.*;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.assertThat;

abstract class ApiSupport {
    @LocalServerPort int port;
    @Autowired ObjectMapper json;
    final HttpClient http = HttpClient.newHttpClient();
    record Result(int status, JsonNode body) {}
    Result request(String method, String path, String token, Object body) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api" + path));
        if (token != null) builder.header("Authorization", "Bearer " + token);
        builder.header("Content-Type", "application/json");
        var payload = body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body));
        var response = http.send(builder.method(method, payload).build(), HttpResponse.BodyHandlers.ofString());
        return new Result(response.statusCode(), response.body().isBlank() ? json.nullNode() : json.readTree(response.body()));
    }
    String login(String email, String password) throws Exception {
        var result = request("POST", "/auth/login", null, Map.of("email",email,"password",password));
        assertThat(result.status()).isEqualTo(200);
        return result.body().path("accessToken").asText();
    }
    String client(String email) throws Exception {
        var result = request("POST", "/auth/register", null, Map.of("name","Cliente", "email",email,"password","client-password"));
        assertThat(result.status()).isEqualTo(201);
        return login(email,"client-password");
    }
}
