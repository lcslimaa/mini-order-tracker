package com.delivery.tracker;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class OrderCreationTests extends ApiSupport {
    @Autowired JdbcTemplate jdbc;
    @Test void calculatesTotalAndPreservesHistoricalPricesAndNames() throws Exception {
        var operator=login("operator@example.com","operator-password");
        var customer=client("purchase@example.com");
        long pizza=product(operator,"Pizza original",12.50), drink=product(operator,"Suco",5);
        var result=request("POST","/orders",customer,Map.of("address","Rua A, 10", "items",List.of(Map.of("productId",pizza,"quantity",2),Map.of("productId",drink,"quantity",1))));
        assertThat(result.status()).isEqualTo(201);
        var body=result.body();
        assertThat(body.path("total").asDouble()).isEqualTo(30.00);
        assertThat(body.path("status").asText()).isEqualTo("RECEBIDO");
        assertThat(body.path("address").asText()).isEqualTo("Rua A, 10");
        assertThat(body.path("items").get(0).path("subtotal").asDouble()).isEqualTo(25.00);
        assertThat(body.path("customer").path("email").asText()).isEqualTo("purchase@example.com");
        assertThat(body.toString()).doesNotContain("password");
        var id=body.path("id").asLong();
        assertThat(request("PUT","/products/"+pizza,operator,Map.of("name","Pizza nova","price",50,"available",false)).status()).isEqualTo(200);
        var original=request("GET","/orders/"+id,customer,null).body();
        assertThat(original.path("total").asDouble()).isEqualTo(30.00);
        assertThat(original.path("items").get(0).path("name").asText()).isEqualTo("Pizza original");
        assertThat(original.path("items").get(0).path("unitPrice").asDouble()).isEqualTo(12.50);
        var failed=request("POST","/orders",customer,Map.of("address","Rua B", "items",List.of(Map.of("productId",pizza,"quantity",1))));
        assertThat(failed.status()).isEqualTo(400);
    }
    @Test void clientsOnlyReadTheirOrdersAndOperatorSeesAll() throws Exception {
        var operator=login("operator@example.com","operator-password");
        var a=client("owner-a@example.com");var b=client("owner-b@example.com");
        assertThat(request("GET","/orders",a,null).body().size()).isZero();
        long product=product(operator,"Item isolado",1);
        long first=order(a,product), second=order(b,product);
        var own=request("GET","/orders",a,null);
        assertThat(own.status()).isEqualTo(200);
        assertThat(own.body().size()).isEqualTo(1);
        assertThat(own.body().get(0).path("id").asLong()).isEqualTo(first);
        var foreign=request("GET","/orders/"+second,a,null);
        assertThat(foreign.status()).isEqualTo(404);
        assertThat(foreign.body().toString()).doesNotContain("owner-b");
        assertThat(request("GET","/orders/9999999",operator,null).status()).isEqualTo(404);
        assertThat(request("GET","/orders/"+first,operator,null).status()).isEqualTo(200);
        var all=request("GET","/orders",operator,null).body();
        assertThat(all.valueStream().map(n->n.path("id").asLong()).toList()).contains(first,second).isSorted();
    }
    @Test void invalidInputsAndForgedFieldsNeverCreatePartialOrders() throws Exception {
        var operator=login("operator@example.com","operator-password");
        var customer=client("invalid-orders@example.com");
        long product=product(operator,"Válido",1);
        var item=Map.of("productId",product,"quantity",1);
        var initialOrders=jdbc.queryForObject("select count(*) from orders",Long.class);
        var initialItems=jdbc.queryForObject("select count(*) from order_items",Long.class);
        for(Object body : new Object[]{Map.of("address","","items",List.of(item)),Map.of("address","Rua","items",List.of()),Map.of("address","Rua","items",List.of(item,item)),Map.of("address","Rua","items",List.of(Map.of("productId",product,"quantity",0))),Map.of("address","Rua","items",List.of(Map.of("productId",product,"quantity",-1))),Map.of("address","Rua","items",List.of(Map.of("productId",product,"quantity",1.5))),Map.of("address","Rua","items",List.of(item,Map.of("productId",9999999,"quantity",1))),Map.of("address","Rua","items",List.of(item),"customerId",1),Map.of("address","Rua","items",List.of(item),"total",0),Map.of("address","Rua","items",List.of(item),"status","ENTREGUE"),Map.of("address","Rua","items",List.of(Map.of("productId",product,"quantity",1,"unitPrice",0)))}) {
            assertThat(request("POST","/orders",customer,body).status()).as(body.toString()).isEqualTo(400);
        }
        assertThat(request("POST","/orders",operator,Map.of("address","Rua","items",List.of(item))).status()).isEqualTo(403);
        assertThat(jdbc.queryForObject("select count(*) from orders",Long.class)).isEqualTo(initialOrders);
        assertThat(jdbc.queryForObject("select count(*) from order_items",Long.class)).isEqualTo(initialItems);
    }
}
