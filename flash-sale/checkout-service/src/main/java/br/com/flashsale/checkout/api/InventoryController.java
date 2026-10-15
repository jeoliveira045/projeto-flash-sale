package br.com.flashsale.checkout.api;

import br.com.flashsale.checkout.domain.StockService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/v1/inventory")
public class InventoryController {
    private final StockService stockService;

    public InventoryController(StockService stockService) {
        this.stockService = stockService;
    }

    @PutMapping("/{productId}")
    public InventoryResponse setStock(
            @PathVariable @Pattern(regexp = "[A-Za-z0-9._-]{1,80}") String productId,
            @Valid @RequestBody InventoryRequest request) {
        stockService.setAvailable(productId, request.quantity());
        return new InventoryResponse(productId, request.quantity());
    }

    @GetMapping("/{productId}")
    public InventoryResponse getStock(
            @PathVariable @Pattern(regexp = "[A-Za-z0-9._-]{1,80}") String productId) {
        return new InventoryResponse(productId, stockService.available(productId));
    }
}