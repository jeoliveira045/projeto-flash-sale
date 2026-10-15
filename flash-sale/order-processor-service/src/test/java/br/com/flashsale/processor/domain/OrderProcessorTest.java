package br.com.flashsale.processor.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.flashsale.contracts.OrderCreatedEvent;
import br.com.flashsale.contracts.OrderStatus;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderProcessorTest {
    @Mock
    private ProcessedOrderRepository repository;

    private OrderProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new OrderProcessor(repository);
        when(repository.findById("order-001")).thenReturn(Optional.empty());
        when(repository.save(any(ProcessedOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void confirmsOrderAndReturnsStatusEvent() {
        var result = processor.process(event(false));

        assertEquals(OrderStatus.CONFIRMED, result.statusEvent().status());
        assertNull(result.compensationEvent());
        verify(repository).save(any(ProcessedOrder.class));
    }

    @Test
    void cancelsOrderAndReturnsCompensationEventWhenFailureIsSimulated() {
        var result = processor.process(event(true));

        assertNull(result.statusEvent());
        assertEquals("Falha de processamento simulada", result.compensationEvent().reason());
        assertEquals(2, result.compensationEvent().quantity());
        verify(repository).save(any(ProcessedOrder.class));
    }

    private OrderCreatedEvent event(boolean fail) {
        return new OrderCreatedEvent("order-001", "PROD-001", 2, "50329291076",
                "Pessoa Teste", fail, "Falha de processamento simulada", "trace-001", Instant.now());
    }
}