package com.onepercentgrowth.local_to_smartapi.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onepercentgrowth.local_to_smartapi.eventhandling.OrderEventQueue;
import com.onepercentgrowth.local_to_smartapi.model.OrderContext;
import com.onepercentgrowth.local_to_smartapi.registry.OrderRegistry;
import com.onepercentgrowth.local_to_smartapi.websocket.OrderStatusResponse;
import org.springframework.beans.factory.annotation.Autowired;
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
    public void testSellandStoploss(@RequestBody OrderStatusResponse response) {

        if (response.getOrderStatusData() == null) {
            throw new IllegalArgumentException("orderStatusData missing in payload");
        }

        if(response.getOrderStatusData().getTransactiontype().equalsIgnoreCase("BUY"))
        {
            OrderContext orderContext = new OrderContext(
                    "260106000810894",   // buyOrderId
                    "260106000810939",   // sellOrderId
                    "260106000811022",   // stopLossOrderId
                    response.getOrderStatusData().getTradingsymbol(),         // tradingSymbol
                    response.getOrderStatusData().getSymboltoken(),              // symbolToken
                    Integer.parseInt(response.getOrderStatusData().getQuantity()),                   // quantity
                    "NORMAL",            // sellVariety
                    "STOPLOSS"           // stopLossVariety
            );

            orderRegistry.registerBuy(orderContext);
        }

        // Publish incoming event
        orderEventQueue.publish(response);
    }
}
