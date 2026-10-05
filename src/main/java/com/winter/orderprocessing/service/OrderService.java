package com.winter.orderprocessing.service;

import com.winter.orderprocessing.dto.CreateOrderRequest;
import com.winter.orderprocessing.dto.OrderItemRequest;
import com.winter.orderprocessing.dto.OrderResponse;
import com.winter.orderprocessing.exception.InvalidOrderStatusException;
import com.winter.orderprocessing.exception.InvalidOrderTransitionException;
import com.winter.orderprocessing.exception.OrderNotFoundException;
import com.winter.orderprocessing.entity.OrderEntity;
import com.winter.orderprocessing.entity.OrderStatus;
import com.winter.orderprocessing.dao.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        OrderEntity order = OrderEntity.pending();
        for (OrderItemRequest item : request.items()) {
            order.addItem(item.productName().trim(), item.quantity(), item.unitPrice());
        }
        return OrderResponse.from(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long id) {
        return OrderResponse.from(findOrder(id));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listOrders(String statusValue) {
        if (statusValue == null || statusValue.isBlank()) {
            return orderRepository.findAllByOrderByCreatedAtDesc().stream().map(OrderResponse::from).toList();
        }
        OrderStatus status = parseStatus(statusValue);
        return orderRepository.findAllByStatusOrderByCreatedAtDesc(status).stream()
                .map(OrderResponse::from)
                .toList();
    }

    @Transactional
    public OrderResponse updateStatus(Long id, String statusValue) {
        OrderEntity order = findOrder(id);
        OrderStatus requested = parseStatus(statusValue);
        if (!order.getStatus().canTransitionTo(requested)) {
            throw new InvalidOrderTransitionException(order.getStatus(), requested);
        }
        order.transitionTo(requested);
        return OrderResponse.from(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse cancelOrder(Long id) {
        findOrder(id);
        int changed = orderRepository.cancelPendingOrder(id, OrderStatus.PENDING, OrderStatus.CANCELLED);
        if (changed == 0) {
            throw new InvalidOrderTransitionException(findOrder(id).getStatus());
        }
        return OrderResponse.from(findOrder(id));
    }

    @Transactional
    public int processPendingOrders() {
        return orderRepository.processPendingOrders(OrderStatus.PENDING, OrderStatus.PROCESSING);
    }

    private OrderEntity findOrder(Long id) {
        return orderRepository.findWithItemsById(id).orElseThrow(() -> new OrderNotFoundException(id));
    }

    private OrderStatus parseStatus(String value) {
        try {
            return OrderStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new InvalidOrderStatusException(value);
        }
    }
}
