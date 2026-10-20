package com.delivery.tracker;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.assertThat;

@org.junit.jupiter.api.TestMethodOrder(org.junit.jupiter.api.MethodOrderer.OrderAnnotation.class)
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, properties="spring.datasource.url=jdbc:h2:mem:catalog-test;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
class CatalogTests extends ApiSupport {
    @Test @org.junit.jupiter.api.Order(0)
    void emptyCatalogIsAnEmptyArray() throws Exception {
        var token=client("empty-catalog@example.com");
        var result=request("GET","/products",token,null);
        assertThat(result.status()).isEqualTo(200);
        assertThat(result.body().isArray()).isTrue();
        assertThat(result.body().size()).isZero();
    }

    @Test void operatorMaintainsProductsAndClientsSeeOnlyAvailableProducts() throws Exception {
        var operator=login("operator@example.com","operator-password");
        var customer=client("catalog@example.com");
        var result=request("POST","/products",operator,Map.of("name","Pizza","price",12.50));
        assertThat(result.status()).isEqualTo(201);
        assertThat(result.body().path("available").asBoolean()).isTrue();
        long id=result.body().path("id").asLong();
        assertThat(request("GET","/products",customer,null).body().findValuesAsString("name")).contains("Pizza");
        result=request("PUT","/products/"+id,operator,Map.of("name","Pizza nova","price",15.00,"available",false));
        assertThat(result.status()).isEqualTo(200);
        assertThat(result.body().path("price").asDouble()).isEqualTo(15.0);
        assertThat(request("GET","/products",customer,null).body().findValuesAsString("name")).doesNotContain("Pizza nova");
        assertThat(request("GET","/products",operator,null).body().findValuesAsString("name")).contains("Pizza nova");
        assertThat(request("POST","/products",customer,Map.of("name","X","price",1)).status()).isEqualTo(403);
        assertThat(request("PUT","/products/"+id,customer,Map.of("name","X","price",1,"available",true)).status()).isEqualTo(403);
        assertThat(request("PUT","/products/9999999",operator,Map.of("name","X","price",1,"available",true)).status()).isEqualTo(404);
    }
    @Test void invalidProductInputsNeverPersist() throws Exception {
        var operator=login("operator@example.com","operator-password");
        for(var body : new Object[]{Map.of("name","","price",1),Map.of("name","X","price",0),Map.of("name","X","price",-1),Map.of("name","X","price",1.001),Map.of("name","X"),Map.of("name","X","price",1,"unknown",true)}) {
            assertThat(request("POST","/products",operator,body).status()).isEqualTo(400);
        }
        var created=request("POST","/products",operator,Map.of("name","Original","price",10,"available",false));
        assertThat(created.status()).isEqualTo(201);
        var id=created.body().path("id").asLong();
        assertThat(request("PUT","/products/"+id,operator,Map.of("name","Broken","price",-1,"available",true)).status()).isEqualTo(400);
        var products=request("GET","/products",operator,null).body();
        assertThat(products.findValuesAsString("name")).contains("Original").doesNotContain("Broken");
    }
}
