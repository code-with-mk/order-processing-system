package com.winter.orderprocessing.controller;

import com.winter.orderprocessing.dto.CreateOrderRequest;
import com.winter.orderprocessing.dto.OrderResponse;
import com.winter.orderprocessing.dto.UpdateOrderStatusRequest;
import com.winter.orderprocessing.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        OrderResponse created = orderService.createOrder(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{id}")
    public OrderResponse getOrder(@PathVariable Long id) {
        return orderService.getOrder(id);
    }

    @GetMapping
    public List<OrderResponse> listOrders(@RequestParam(required = false) String status) {
        return orderService.listOrders(status);
    }

    @PatchMapping("/{id}/status")
    public OrderResponse updateStatus(@PathVariable Long id,
                                     @Valid @RequestBody UpdateOrderStatusRequest request) {
        return orderService.updateStatus(id, request.status());
    }

    @PostMapping("/{id}/cancel")
    public OrderResponse cancelOrder(@PathVariable Long id) {
        return orderService.cancelOrder(id);
    }
}
