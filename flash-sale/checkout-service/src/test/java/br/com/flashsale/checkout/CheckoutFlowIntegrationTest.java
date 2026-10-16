package br.com.flashsale.checkout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.flashsale.checkout.api.CreateOrderAccepted;
import br.com.flashsale.checkout.api.CreateOrderRequest;
import br.com.flashsale.checkout.api.InventoryRequest;
import br.com.flashsale.checkout.api.InventoryResponse;
import br.com.flashsale.checkout.api.OrderResponse;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnabledIfSystemProperty(named = "runLocalIntegration", matches = "true")
class CheckoutFlowIntegrationTest {
    @Autowired
    private TestRestTemplate http;
    @Test
    void completesSuccessAndCompensationFlowsWithCorrelationId() throws InterruptedException {
        String productId = "IT-" + UUID.randomUUID().toString().substring(0, 8);
        String successCorrelationId = UUID.randomUUID().toString();
        setStock(productId, 10);

        CreateOrderAccepted accepted = createOrder(productId, false, successCorrelationId);
        assertEquals("PROCESSING", accepted.status());
        OrderResponse confirmed = waitForStatus(accepted.orderId(), "CONFIRMED", successCorrelationId);
        assertEquals(successCorrelationId, confirmed.correlationId());

        setStock(productId, 10);
        String failureCorrelationId = UUID.randomUUID().toString();
        CreateOrderAccepted failed = createOrder(productId, true, failureCorrelationId);
        OrderResponse cancelled = waitForStatus(failed.orderId(), "CANCELLED", failureCorrelationId);
        assertEquals(failureCorrelationId, cancelled.correlationId());
        assertEquals(10, getStock(productId).available());
    }

    private void setStock(String productId, int quantity) {
        var response = http.exchange("/v1/inventory/" + productId, HttpMethod.PUT,
                jsonEntity(new InventoryRequest(quantity)), InventoryResponse.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    private InventoryResponse getStock(String productId) {
        return http.getForObject("/v1/inventory/" + productId, InventoryResponse.class);
    }

    private CreateOrderAccepted createOrder(String productId, boolean fail, String correlationId) {
        var request = new CreateOrderRequest(productId, 2, "50329291076", "Integracao",
                fail, "Falha de integracao simulada");
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Correlation-Id", correlationId);
        ResponseEntity<CreateOrderAccepted> response = http.postForEntity("/v1/orders",
                new HttpEntity<>(request, headers), CreateOrderAccepted.class);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(correlationId, response.getHeaders().getFirst("X-Correlation-Id"));
        return response.getBody();
    }

        private OrderResponse waitForStatus(String orderId, String expected, String correlationId) throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(15).toNanos();
        do {
            var headers = new HttpHeaders();
            headers.set("X-Correlation-Id", correlationId);
            ResponseEntity<OrderResponse> entity = http.exchange("/v1/orders/" + orderId, HttpMethod.GET,
                new HttpEntity<>(headers), OrderResponse.class);
            OrderResponse response = entity.getBody();
            if (entity.getStatusCode().is2xxSuccessful() && response != null && response.status() != null
                    && expected.equals(response.status().name())) {
                return response;
            }
            Thread.sleep(100);
        } while (System.nanoTime() < deadline);
        throw new AssertionError("Pedido " + orderId + " nao chegou a " + expected);
    }

    private HttpEntity<?> jsonEntity(Object body) {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }
}