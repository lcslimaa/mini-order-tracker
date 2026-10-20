package com.delivery.tracker;

import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.assertThat;

class PersistenceFlowTests extends ApiSupport {
    ConfigurableApplicationContext open(Path file,String bootstrapPassword) {
        var context=new SpringApplicationBuilder(MiniOrderTrackerApplication.class).run(
            "--server.port=0", "--spring.datasource.url=jdbc:h2:file:"+file,
            "--app.jwt.secret=persistence-test-secret-012345678901234567890123456789",
            "--app.operator.name=Operador", "--app.operator.email=operator-persist@example.com",
            "--app.operator.password="+bootstrapPassword,"--spring.main.banner-mode=off");
        port=((ServletWebServerApplicationContext)context).getWebServer().getPort();
        json=context.getBean(ObjectMapper.class);
        return context;
    }
    @Test void completeHttpFlowSurvivesClosingAndReopeningSameFile(@TempDir Path directory) throws Exception {
        var file=directory.resolve("delivery");
        long delivered,earlyCancelled,preparationCancelled,deliveryCancelled,pizza;
        String originalToken;
        try(var context=open(file,"original-password")) {
            var operator=login("operator-persist@example.com","original-password");
            originalToken=client("persistent-owner@example.com");
            var stranger=client("persistent-other@example.com");
            assertThat(request("GET","/products",originalToken,null).body().size()).isZero();
            pizza=product(operator,"Pizza histórica",12.50);var drink=product(operator,"Suco histórico",5);
            var purchase=request("POST","/orders",originalToken,Map.of("address","Rua Persistida, 10","items",List.of(Map.of("productId",pizza,"quantity",2),Map.of("productId",drink,"quantity",1))));
            assertThat(purchase.status()).isEqualTo(201);
            delivered=purchase.body().path("id").asLong();
            assertThat(purchase.body().path("total").asDouble()).isEqualTo(30.0);
            assertThat(request("GET","/orders/"+delivered,stranger,null).status()).isEqualTo(404);
            for(String state : List.of("EM_PREPARO","SAIU_PARA_ENTREGA","ENTREGUE")) {
                assertThat(request("PATCH","/orders/"+delivered+"/status",operator,Map.of("status",state)).status()).isEqualTo(200);
            }
            assertThat(request("POST","/orders/"+delivered+"/cancel",operator,null).status()).isEqualTo(409);
            earlyCancelled=order(originalToken,pizza);
            assertThat(request("POST","/orders/"+earlyCancelled+"/cancel",originalToken,null).status()).isEqualTo(200);
            preparationCancelled=order(originalToken,pizza);
            assertThat(request("PATCH","/orders/"+preparationCancelled+"/status",operator,Map.of("status","EM_PREPARO")).status()).isEqualTo(200);
            assertThat(request("POST","/orders/"+preparationCancelled+"/cancel",originalToken,null).status()).isEqualTo(409);
            assertThat(request("POST","/orders/"+preparationCancelled+"/cancel",operator,null).status()).isEqualTo(200);
            deliveryCancelled=order(stranger,drink);
            for(String state : List.of("EM_PREPARO","SAIU_PARA_ENTREGA")) {
                assertThat(request("PATCH","/orders/"+deliveryCancelled+"/status",operator,Map.of("status",state)).status()).isEqualTo(200);
            }
            assertThat(request("POST","/orders/"+deliveryCancelled+"/cancel",operator,null).status()).isEqualTo(200);
            assertThat(request("PUT","/products/"+pizza,operator,Map.of("name","Pizza alterada","price",99,"available",false)).status()).isEqualTo(200);
            assertThat(request("GET","/orders",operator,null).body().size()).isEqualTo(4);
        }
        assertThat(directory.resolve("delivery.mv.db")).exists();
        try(var context=open(file,"replacement-password")) {
            var operator=login("operator-persist@example.com","original-password");
            assertThat(request("POST","/auth/login",null,Map.of("email","operator-persist@example.com","password","replacement-password")).status()).isEqualTo(401);
            var customer=login("persistent-owner@example.com","client-password");
            assertThat(request("GET","/orders",customer,null).body().size()).isEqualTo(3);
            var restored=request("GET","/orders/"+delivered,originalToken,null);
            assertThat(restored.status()).isEqualTo(200);
            assertThat(restored.body().path("status").asText()).isEqualTo("ENTREGUE");
            assertThat(restored.body().path("address").asText()).isEqualTo("Rua Persistida, 10");
            assertThat(restored.body().path("total").asDouble()).isEqualTo(30.0);
            assertThat(restored.body().path("items").get(0).path("name").asText()).isEqualTo("Pizza histórica");
            assertThat(restored.body().path("items").get(0).path("unitPrice").asDouble()).isEqualTo(12.50);
            for(long id : List.of(earlyCancelled,preparationCancelled,deliveryCancelled)) {
                assertThat(request("GET","/orders/"+id,operator,null).body().path("status").asText()).isEqualTo("CANCELADO");
            }
            assertThat(request("GET","/orders",operator,null).body().size()).isEqualTo(4);
            assertThat(request("GET","/products",operator,null).body().size()).isEqualTo(2);
            assertThat(request("GET","/products",customer,null).body().size()).isEqualTo(1);
            assertThat(request("POST","/orders",customer,Map.of("address","Rua","items",List.of(Map.of("productId",pizza,"quantity",1)))).status()).isEqualTo(400);
            assertThat(context.getBean(JdbcTemplate.class).queryForObject("select count(*) from app_users",Long.class)).isEqualTo(3);
        }
    }
}
