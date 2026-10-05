package com.winter.orderprocessing;

import com.winter.orderprocessing.service.OrderService;
import com.winter.orderprocessing.dao.OrderRepository;
import com.winter.orderprocessing.scheduler.PendingOrderScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderProcessingApiTests {

    private static final String ORDERS_PATH = "/api/v1/orders";
    private static final String VALID_ORDER = """
            {"items":[{"productName":"Keyboard","quantity":2,"unitPrice":14.25}]}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderService orderService;

    @Autowired
    private PendingOrderScheduler pendingOrderScheduler;

    @BeforeEach
    void clearOrders() {
        orderRepository.deleteAll();
    }

    @Test
    void createsOrderWithPendingStatusAndCalculatedTotals() throws Exception {
        mockMvc.perform(post(ORDERS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_ORDER))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].lineTotal").value(28.50))
                .andExpect(jsonPath("$.total").value(28.50));
    }

    @Test
    void createsAnOrderWithMultipleItems() throws Exception {
        String multipleItems = """
                {"items":[
                  {"productName":"Keyboard","quantity":2,"unitPrice":14.25},
                  {"productName":"Cable","quantity":1,"unitPrice":3.00}
                ]}
                """;

        mockMvc.perform(post(ORDERS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(multipleItems))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.total").value(31.50));
    }

    @Test
    void rejectsInvalidOrderItemsWithConsistentErrorBody() throws Exception {
        String invalidOrder = """
                {"items":[{"productName":" ","quantity":0,"unitPrice":-1}]}
                """;

        mockMvc.perform(post(ORDERS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidOrder))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void returnsNotFoundForUnknownOrder() throws Exception {
        mockMvc.perform(get(ORDERS_PATH + "/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void listsOrdersFilteredByStatus() throws Exception {
        long pendingId = createOrder();
        long cancelledId = createOrder();
        mockMvc.perform(post(ORDERS_PATH + "/" + cancelledId + "/cancel"))
                .andExpect(status().isOk());

        mockMvc.perform(get(ORDERS_PATH).param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(pendingId));
    }

    @Test
    void allowsOnlyForwardStatusTransitions() throws Exception {
        long orderId = createOrder();
        updateStatus(orderId, "SHIPPED")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        orderService.processPendingOrders();
        updateStatus(orderId, "SHIPPED").andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHIPPED"));
        updateStatus(orderId, "DELIVERED").andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED"));
        updateStatus(orderId, "PROCESSING").andExpect(status().isBadRequest());
    }

    @Test
    void cancellationIsAllowedOnlyWhilePending() throws Exception {
        long orderId = createOrder();

        mockMvc.perform(post(ORDERS_PATH + "/" + orderId + "/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        mockMvc.perform(post(ORDERS_PATH + "/" + orderId + "/cancel"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void pendingProcessorMovesPendingOrdersToProcessing() throws Exception {
        long orderId = createOrder();

        pendingOrderScheduler.processPendingOrders();

        mockMvc.perform(get(ORDERS_PATH + "/" + orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROCESSING"));
    }

    @Test
    void pendingProcessorDoesNotOverwriteCancelledOrders() throws Exception {
        long orderId = createOrder();
        mockMvc.perform(post(ORDERS_PATH + "/" + orderId + "/cancel"))
                .andExpect(status().isOk());

        pendingOrderScheduler.processPendingOrders();

        mockMvc.perform(get(ORDERS_PATH + "/" + orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void rejectsUnknownStatusFilterWithConsistentErrorBody() throws Exception {
        mockMvc.perform(get(ORDERS_PATH).param("status", "READY"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    private long createOrder() throws Exception {
        MvcResult result = mockMvc.perform(post(ORDERS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_ORDER))
                .andExpect(status().isCreated())
                .andReturn();
        String location = result.getResponse().getHeader("Location");
        return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
    }

    private org.springframework.test.web.servlet.ResultActions updateStatus(long orderId, String newStatus) throws Exception {
        return mockMvc.perform(patch(ORDERS_PATH + "/" + orderId + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"" + newStatus + "\"}"));
    }
}
