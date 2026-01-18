package com.onepercentgrowth.local_to_smartapi.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onepercentgrowth.local_to_smartapi.config.TokenManager;
import com.onepercentgrowth.local_to_smartapi.eventhandling.OrderEventQueue;
import com.onepercentgrowth.local_to_smartapi.model.OrderContext;
import com.onepercentgrowth.local_to_smartapi.registry.OrderRegistry;
import com.onepercentgrowth.local_to_smartapi.service.OrderExecutionService;
import com.onepercentgrowth.local_to_smartapi.websocket.OrderStatusResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Optional;

@RestController
public class TestController {

    private static final Logger log = LoggerFactory.getLogger(TestController.class);

    @Autowired
    private OrderEventQueue orderEventQueue;
    @Autowired
    private OrderRegistry orderRegistry;
    @Autowired
    private OrderExecutionService orderExecutionService;
    @Autowired
    private TokenManager tokenManager;

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

        Optional<OrderContext> existingCtx =
                orderRegistry.getByAnyOrderId(response.getOrderStatusData().getOrderid());

        if (existingCtx.isPresent()) {
            // BUY already processed → do nothing / reuse
            log.info("OrderContext already exists for orderId={}", response.getOrderStatusData().getOrderid());
            orderEventQueue.publish(response);
            return;
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

    @GetMapping("/modify")
    public void modify()
    {
        String tradingSymbol = "TATASTEEL-EQ";
        String symbolToken = "3499";
        int quantity = 10;
        double price = 100.0;

        orderExecutionService
                .placeStopLossOrder(
                        tradingSymbol,
                        symbolToken,
                        quantity,
                        10,
                        price,
                        tokenManager.getValidJwtToken()
                )
                .flatMap(orderResponse ->
                        orderExecutionService.modifyStopLossOrder(
                                tradingSymbol,
                                symbolToken,
                                quantity+10,
                                0,
                                price, // new price (can be different)
                                orderResponse.getData().getOrderid(),
                                tokenManager.getValidJwtToken()
                        )
                )
                .doOnSuccess(resp ->
                        log.info("SELL order modified successfully orderId={}",
                                resp.getData().getOrderid())
                )
                .doOnError(err ->
                        log.error("SELL place/modify flow failed", err)
                )
                .onErrorResume(err -> Mono.empty())
                .subscribe();
    }
}
