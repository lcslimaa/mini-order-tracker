package com.delivery.tracker;

import java.net.URI;
import java.net.http.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CorsTests extends ApiSupport {
    @Test void allowsLocalPreflightForAllBrowserOperations() throws Exception {
        for(var route : new String[][]{{"GET","/api/auth/me"},{"POST","/api/auth/login"},{"POST","/api/auth/register"},{"GET","/api/products"},{"POST","/api/products"},{"PUT","/api/products/1"},{"GET","/api/orders"},{"POST","/api/orders"},{"PATCH","/api/orders/1/status"},{"POST","/api/orders/1/cancel"}}) {
            var req=HttpRequest.newBuilder(URI.create("http://localhost:"+port+route[1]))
                .header("Origin","http://localhost:3000").header("Access-Control-Request-Method",route[0])
                .header("Access-Control-Request-Headers","authorization,content-type").method("OPTIONS",HttpRequest.BodyPublishers.noBody()).build();
            var res=http.send(req,HttpResponse.BodyHandlers.ofString());
            assertThat(res.statusCode()).as(route[0]+route[1]).isEqualTo(200);
            assertThat(res.headers().firstValue("Access-Control-Allow-Origin")).contains("http://localhost:3000");
        }
    }
    @Test void rejectsOtherOriginsAndKeepsAuthorization() throws Exception {
        var denied=HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/api/orders"))
            .header("Origin","http://untrusted.example").header("Access-Control-Request-Method","GET")
            .method("OPTIONS",HttpRequest.BodyPublishers.noBody()).build();
        var res=http.send(denied,HttpResponse.BodyHandlers.ofString());
        assertThat(res.headers().firstValue("Access-Control-Allow-Origin")).isEmpty();
        var req=HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/api/orders"))
            .header("Origin","http://localhost:3000").GET().build();
        var anonymous=http.send(req,HttpResponse.BodyHandlers.ofString());
        assertThat(anonymous.statusCode()).isEqualTo(401);
        assertThat(anonymous.headers().firstValue("Access-Control-Allow-Origin")).contains("http://localhost:3000");
        assertThat(request("POST","/products",client("cors-client@example.com"),java.util.Map.of()).status()).isEqualTo(403);
    }
}
