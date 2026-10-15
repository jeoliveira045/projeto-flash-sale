package br.com.flashsale.checkout.api;

import br.com.flashsale.checkout.domain.OrderService;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CreateOrderAccepted create(@Valid @RequestBody CreateOrderRequest request) {
        var order = orderService.create(request, MDC.get("correlationId"));
        return new CreateOrderAccepted(order.orderId(), order.status().name(),
                order.updatedAt(), order.correlationId());
    }

    @GetMapping("/{orderId}")
    public OrderResponse findById(@PathVariable String orderId) {
        return orderService.findById(orderId);
    }
}