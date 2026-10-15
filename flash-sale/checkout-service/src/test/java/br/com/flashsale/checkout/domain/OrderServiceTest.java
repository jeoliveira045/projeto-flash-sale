package br.com.flashsale.checkout.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.flashsale.checkout.api.CreateOrderRequest;
import br.com.flashsale.checkout.messaging.OrderEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Mock
    private StockService stockService;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderEventPublisher publisher;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(stockService, orderRepository, publisher);
    }

    @Test
    void createsProcessingOrderWhenStockIsReservedAndMessageIsConfirmed() {
        when(stockService.reserve("PROD-001", 2)).thenReturn(true);

        var response = orderService.create(request(false), "trace-001");

        assertEquals("PROCESSING", response.status().name());
        assertEquals("trace-001", response.correlationId());
        verify(orderRepository).save(any(OrderEntity.class));
        verify(publisher).publish(any());
    }

    @Test
    void rejectsOrderWhenStockIsInsufficient() {
        when(stockService.reserve("PROD-001", 2)).thenReturn(false);

        var exception = assertThrows(ResponseStatusException.class,
                () -> orderService.create(request(false), "trace-002"));

        assertEquals(409, exception.getStatusCode().value());
        verify(publisher, never()).publish(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void returnsReservedStockWhenRabbitPublishFails() {
        when(stockService.reserve("PROD-001", 2)).thenReturn(true);
        org.mockito.Mockito.doThrow(new IllegalStateException("broker indisponivel"))
                .when(publisher).publish(any());

        var exception = assertThrows(ResponseStatusException.class,
                () -> orderService.create(request(false), "trace-003"));

        assertEquals(503, exception.getStatusCode().value());
        verify(stockService).compensate(any(), org.mockito.ArgumentMatchers.eq("PROD-001"),
                org.mockito.ArgumentMatchers.eq(2));
    }

            @Test
            void looksUpMissingOrderThreeTimesBeforeReturningNotFound() {
            when(orderRepository.findById("missing-order"))
                .thenReturn(java.util.Optional.empty())
                .thenReturn(java.util.Optional.empty())
                .thenReturn(java.util.Optional.empty());

            var exception = assertThrows(ResponseStatusException.class,
                () -> orderService.findById("missing-order"));

            assertEquals(404, exception.getStatusCode().value());
            verify(orderRepository, times(3)).findById("missing-order");
            }

    private CreateOrderRequest request(boolean simulateFailure) {
        return new CreateOrderRequest("PROD-001", 2, "50329291076", "Pessoa Teste",
                simulateFailure, "Falha demonstrativa");
    }
}