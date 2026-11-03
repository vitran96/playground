package com.example.cartsm;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderService orderService;

    @Test
    void postOrderReturns200WithCartState() throws Exception {
        mockMvc.perform(post("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.state", is("CART")))
                .andExpect(jsonPath("$.itemCount", is(0)))
                .andExpect(jsonPath("$.allowedEvents", containsInAnyOrder("ADD_ITEM", "CHECKOUT", "CANCEL")));
    }

    @Test
    void illegalTransitionReturns409() throws Exception {
        Order order = orderService.create();

        // SHIP directly from CART is illegal
        mockMvc.perform(post("/api/orders/" + order.getId() + "/events/SHIP"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", is("Event SHIP is not allowed from state CART")));
    }

    @Test
    void notFoundReturns404() throws Exception {
        mockMvc.perform(get("/api/orders/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("Order not found: 99999")));
    }
}
