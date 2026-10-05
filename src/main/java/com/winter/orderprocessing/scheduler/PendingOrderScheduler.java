package com.winter.orderprocessing.scheduler;

import com.winter.orderprocessing.service.OrderService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PendingOrderScheduler {

    private final OrderService orderService;

    public PendingOrderScheduler(OrderService orderService) {
        this.orderService = orderService;
    }

    @Scheduled(fixedRate = 300_000L)
    public void processPendingOrders() {
        orderService.processPendingOrders();
    }
}
