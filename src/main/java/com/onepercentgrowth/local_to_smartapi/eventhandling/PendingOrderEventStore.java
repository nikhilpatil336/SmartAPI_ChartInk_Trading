package com.onepercentgrowth.local_to_smartapi.eventhandling;

import com.onepercentgrowth.local_to_smartapi.websocket.OrderStatusResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

@Component
public class PendingOrderEventStore {

    private static final Logger log =
            LoggerFactory.getLogger(PendingOrderEventStore.class);

//    @Autowired
//    private OrderEventDispatcher dispatcher;

    private final ConcurrentMap<String,
                Queue<OrderStatusResponse>> pendingEvents
            = new ConcurrentHashMap<>();


    public void add(
            String orderId,
            OrderStatusResponse response
    ) {

        pendingEvents
                .computeIfAbsent(
                        orderId,
                        k -> new ConcurrentLinkedQueue<>()
                )
                .add(response);

        log.info(
                "Buffered pending WS event | orderId={} | status={}",
                orderId,
                response.getOrderStatusData().getStatus()
        );
    }


    public Queue<OrderStatusResponse> remove(String orderId) {
        return pendingEvents.remove(orderId);
    }


    public boolean hasPending(String orderId) {
        return pendingEvents.containsKey(orderId);
    }

//    public void replayPendingEvents(String orderId) {
//
//        Queue<OrderStatusResponse> events =
//                pendingEvents.remove(orderId);
//
//        if (events == null || events.isEmpty()) {
//            return;
//        }
//
//        log.info(
//                "Replaying {} pending WS events for orderId={}",
//                events.size(),
//                orderId
//        );
//
//        events.forEach(dispatcher::dispatchDirectly);
//    }
}
