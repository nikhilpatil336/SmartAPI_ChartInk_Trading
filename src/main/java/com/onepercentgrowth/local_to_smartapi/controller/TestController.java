package com.onepercentgrowth.local_to_smartapi.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onepercentgrowth.local_to_smartapi.eventhandling.OrderEventQueue;
import com.onepercentgrowth.local_to_smartapi.model.OrderContext;
import com.onepercentgrowth.local_to_smartapi.registry.OrderRegistry;
import com.onepercentgrowth.local_to_smartapi.websocket.OrderStatusResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @Autowired
    private OrderEventQueue orderEventQueue;
    @Autowired
    private OrderRegistry orderRegistry;

    private final ObjectMapper mapper = new ObjectMapper();

    @PostMapping("/test")
    public void test(@RequestBody String payload) throws JsonProcessingException {
        OrderStatusResponse response =
                mapper.readValue(payload, OrderStatusResponse.class);

        orderEventQueue.publish(response);
    }

    @PostMapping("/testSellandStoploss")
    public void testSellandStoploss(@RequestBody String payload) throws JsonProcessingException {

        OrderStatusResponse response =
                mapper.readValue(payload, OrderStatusResponse.class);

        orderRegistry.registerBuy(new OrderContext(response.getOrderStatusData().getOrderid(), response.getOrderStatusData().getTradingsymbol(), "3499", Integer.parseInt(response.getOrderStatusData().getQuantity())));

        orderEventQueue.publish(response);
    }

}
