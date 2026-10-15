package br.com.flashsale.checkout.web;

import br.com.flashsale.checkout.api.CreateOrderRequest;
import br.com.flashsale.checkout.domain.OrderService;
import br.com.flashsale.checkout.domain.StockService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.slf4j.MDC;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@Validated
public class DemoController {
    private final StockService stockService;
    private final OrderService orderService;

    public DemoController(StockService stockService, OrderService orderService) {
        this.stockService = stockService;
        this.orderService = orderService;
    }

    @GetMapping("/")
    public String home(@RequestParam(defaultValue = "PROD-001") String productId, Model model) {
        model.addAttribute("productId", productId);
        model.addAttribute("available", stockService.available(productId));
        return "index";
    }

    @PostMapping("/demo/stock")
    public String updateStock(@RequestParam @Pattern(regexp = "[A-Za-z0-9._-]{1,80}") String productId,
                              @RequestParam @PositiveOrZero long quantity,
                              RedirectAttributes redirectAttributes) {
        stockService.setAvailable(productId, quantity);
        redirectAttributes.addAttribute("productId", productId);
        redirectAttributes.addFlashAttribute("message", "Estoque atualizado para " + quantity + " unidades.");
        return "redirect:/";
    }

    @PostMapping("/demo/orders")
    public String createOrder(@RequestParam @Pattern(regexp = "[A-Za-z0-9._-]{1,80}") String productId,
                              @RequestParam @Positive int quantity,
                              @RequestParam @NotBlank String customerName,
                              @RequestParam @Pattern(regexp = "\\d{11}") String customerCpf,
                              @RequestParam(defaultValue = "false") boolean simulateFailure,
                              @RequestParam(required = false) String failureReason,
                              RedirectAttributes redirectAttributes) {
        redirectAttributes.addAttribute("productId", productId);
        try {
            var result = orderService.create(new CreateOrderRequest(productId, quantity, customerCpf,
                    customerName, simulateFailure, failureReason), MDC.get("correlationId"));
            redirectAttributes.addFlashAttribute("message", "Pedido aceito para processamento.");
            redirectAttributes.addFlashAttribute("order", result);
        } catch (ResponseStatusException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getReason());
        }
        return "redirect:/";
    }
}