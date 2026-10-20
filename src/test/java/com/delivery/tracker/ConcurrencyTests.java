package com.delivery.tracker;

import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import javax.sql.DataSource;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ConcurrencyTests extends ApiSupport {
    @Autowired DataSource dataSource;
    @Autowired JdbcTemplate jdbc;
    void awaitBlocked(int count) throws Exception {
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
        while(System.nanoTime()<deadline) {
            int actual=jdbc.queryForObject("select count(*) from information_schema.sessions where blocker_id is not null",Integer.class);
            if(actual>=count) return;
            Thread.sleep(20);
        }
        throw new AssertionError("Requisições não chegaram ao bloqueio esperado: "+count);
    }
    @Test void competingPreparationAndCancellationProduceOneWinnerAndOneConflict() throws Exception {
        var operator=login("operator@example.com","operator-password");
        var customer=client("race-orders@example.com");
        var id=order(customer,product(operator,"Disputa",1));
        try(var connection=dataSource.getConnection();var pool=Executors.newVirtualThreadPerTaskExecutor()) {
            connection.setAutoCommit(false);
            try(var statement=connection.prepareStatement("select id from orders where id=? for update")) { statement.setLong(1,id);statement.executeQuery().close(); }
            var preparation=pool.submit(()->request("PATCH","/orders/"+id+"/status",operator,Map.of("status","EM_PREPARO")));
            var cancellation=pool.submit(()->request("POST","/orders/"+id+"/cancel",customer,null));
            try { awaitBlocked(2); } finally { connection.commit(); }
            var first=preparation.get(10,TimeUnit.SECONDS);var second=cancellation.get(10,TimeUnit.SECONDS);
            assertThat(List.of(first.status(),second.status())).containsExactlyInAnyOrder(200,409);
            var expected=first.status()==200?"EM_PREPARO":"CANCELADO";
            assertThat(request("GET","/orders/"+id,customer,null).body().path("status").asText()).isEqualTo(expected);
        }
    }
    @Test void concurrentProductEditAndPurchasePreserveCoherentSnapshot() throws Exception {
        var operator=login("operator@example.com","operator-password");
        var customer=client("race-catalog@example.com");
        var product=product(operator,"Original concorrente",12.50);
        try(var connection=dataSource.getConnection();var pool=Executors.newVirtualThreadPerTaskExecutor()) {
            connection.setAutoCommit(false);
            try(var statement=connection.prepareStatement("select id from products where id=? for update")) { statement.setLong(1,product);statement.executeQuery().close(); }
            var purchase=pool.submit(()->request("POST","/orders",customer,Map.of("address","Rua concorrente","items",List.of(Map.of("productId",product,"quantity",2)))));
            var edit=pool.submit(()->request("PUT","/products/"+product,operator,Map.of("name","Novo concorrente","price",20,"available",true)));
            try { awaitBlocked(2); } finally { connection.commit(); }
            var bought=purchase.get(10,TimeUnit.SECONDS);assertThat(bought.status()).isEqualTo(201);
            assertThat(edit.get(10,TimeUnit.SECONDS).status()).isEqualTo(200);
            var item=bought.body().path("items").get(0);
            if(item.path("name").asText().equals("Original concorrente")) {
                assertThat(item.path("unitPrice").asDouble()).isEqualTo(12.50);
                assertThat(bought.body().path("total").asDouble()).isEqualTo(25.00);
            } else {
                assertThat(item.path("name").asText()).isEqualTo("Novo concorrente");
                assertThat(item.path("unitPrice").asDouble()).isEqualTo(20.00);
                assertThat(bought.body().path("total").asDouble()).isEqualTo(40.00);
            }
            assertThat(request("GET","/orders/"+bought.body().path("id").asLong(),customer,null).body().path("items")).isEqualTo(bought.body().path("items"));
        }
    }
}
