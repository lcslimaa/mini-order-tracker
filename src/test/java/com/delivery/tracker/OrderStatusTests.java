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
class OrderStatusTests extends ApiSupport {
    @Autowired JdbcTemplate jdbc;
    @Test void operatorTransitionMatrixRejectsSkipsRegressionsRepetitionsAndFinalStates() throws Exception {
        var operator=login("operator@example.com","operator-password");
        var customer=client("matrix@example.com");var product=product(operator,"Matriz",1);
        var states=List.of("RECEBIDO","EM_PREPARO","SAIU_PARA_ENTREGA","ENTREGUE","CANCELADO");
        var permitted=Set.of("RECEBIDO:EM_PREPARO","EM_PREPARO:SAIU_PARA_ENTREGA","SAIU_PARA_ENTREGA:ENTREGUE","RECEBIDO:CANCELADO","EM_PREPARO:CANCELADO","SAIU_PARA_ENTREGA:CANCELADO");
        for(var initial : states) for(var target : states) {
            var id=order(customer,product);jdbc.update("update orders set status=? where id=?",initial,id);
            var result=request("PATCH","/orders/"+id+"/status",operator,Map.of("status",target));
            boolean valid=permitted.contains(initial+":"+target);
            assertThat(result.status()).as(initial+" -> "+target).isEqualTo(valid?200:409);
            assertThat(request("GET","/orders/"+id,customer,null).body().path("status").asText()).isEqualTo(valid?target:initial);
        }
        var id=order(customer,product);
        assertThat(request("PATCH","/orders/"+id+"/status",operator,Map.of("status","INVALIDO")).status()).isEqualTo(400);
        assertThat(request("PATCH","/orders/"+id+"/status",customer,Map.of("status","EM_PREPARO")).status()).isEqualTo(403);
        assertThat(request("PATCH","/orders/99999999/status",operator,Map.of("status","EM_PREPARO")).status()).isEqualTo(404);
    }
    @Test void cancellationsRespectOwnerRoleAndEveryState() throws Exception {
        var operator=login("operator@example.com","operator-password");
        var customer=client("cancel-owner@example.com"); var stranger=client("cancel-stranger@example.com");
        var product=product(operator,"Cancelamento",1);
        for(var state : List.of("RECEBIDO","EM_PREPARO","SAIU_PARA_ENTREGA","ENTREGUE","CANCELADO")) {
            for(boolean byOperator : List.of(false,true)) {
                var id=order(customer,product);jdbc.update("update orders set status=? where id=?",state,id);
                assertThat(request("POST","/orders/"+id+"/cancel",stranger,null).status()).isEqualTo(404);
                boolean valid=byOperator ? List.of("RECEBIDO","EM_PREPARO","SAIU_PARA_ENTREGA").contains(state) : state.equals("RECEBIDO");
                var result=request("POST","/orders/"+id+"/cancel",byOperator?operator:customer,null);
                assertThat(result.status()).as(state+" operator="+byOperator).isEqualTo(valid?200:409);
                assertThat(request("GET","/orders/"+id,customer,null).body().path("status").asText()).isEqualTo(valid?"CANCELADO":state);
            }
        }
        assertThat(request("POST","/orders/99999999/cancel",customer,null).status()).isEqualTo(404);
    }
}
