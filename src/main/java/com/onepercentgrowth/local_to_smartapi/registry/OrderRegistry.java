package com.onepercentgrowth.local_to_smartapi.registry;

import com.onepercentgrowth.local_to_smartapi.model.OrderContext;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

//@Component
//public class OrderRegistry {
//
//    // buyOrderId -> context
//    private final ConcurrentMap<String, OrderContext> byBuyId = new ConcurrentHashMap<>();
//
//    // sellOrderId -> context
//    private final ConcurrentMap<String, OrderContext> bySellId = new ConcurrentHashMap<>();
//
//    // stopLossOrderId -> context
//    private final ConcurrentMap<String, OrderContext> bySlId = new ConcurrentHashMap<>();
//
//    public void register(OrderContext ctx) {
//        byBuyId.put(ctx.getBuyOrderId(), ctx);
//        bySellId.put(ctx.getSellOrderId(), ctx);
//        bySlId.put(ctx.getStopLossOrderId(), ctx);
//    }
//
//    public Optional<OrderContext> getBySellId(String sellOrderId) {
//        return Optional.ofNullable(bySellId.get(sellOrderId));
//    }
//
//    public Optional<OrderContext> getBySlId(String slOrderId) {
//        return Optional.ofNullable(bySlId.get(slOrderId));
//    }
//
//    public void remove(OrderContext ctx) {
//        byBuyId.remove(ctx.getBuyOrderId());
//        bySellId.remove(ctx.getSellOrderId());
//        bySlId.remove(ctx.getStopLossOrderId());
//    }
//}

@Component
public class OrderRegistry {

    private final ConcurrentMap<String, OrderContext> byBuyId = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, OrderContext> bySellId = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, OrderContext> bySlId = new ConcurrentHashMap<>();

    // Phase 1: BUY registered
    public void registerBuy(OrderContext ctx) {
        byBuyId.put(ctx.getBuyOrderId(), ctx);
    }

    public void register(OrderContext ctx) {
        byBuyId.put(ctx.getBuyOrderId(), ctx);
        bySellId.put(ctx.getSellOrderId(), ctx);
        bySlId.put(ctx.getStopLossOrderId(), ctx);
    }

    // Phase 2: SELL + SL registered
    public void registerSellAndSl(OrderContext ctx) {
        if (ctx.getSellOrderId() != null) {
            bySellId.put(ctx.getSellOrderId(), ctx);
        }
        if (ctx.getStopLossOrderId() != null) {
            bySlId.put(ctx.getStopLossOrderId(), ctx);
        }
    }

    public Optional<OrderContext> getByBuyId(String buyOrderId) {
        return Optional.ofNullable(byBuyId.get(buyOrderId));
    }

    public Optional<OrderContext> getBySellId(String sellOrderId) {
        return Optional.ofNullable(bySellId.get(sellOrderId));
    }

    public Optional<OrderContext> getBySlId(String slOrderId) {
        return Optional.ofNullable(bySlId.get(slOrderId));
    }

    public void remove(OrderContext ctx) {
        byBuyId.remove(ctx.getBuyOrderId());
        if (ctx.getSellOrderId() != null) {
            bySellId.remove(ctx.getSellOrderId());
        }
        if (ctx.getStopLossOrderId() != null) {
            bySlId.remove(ctx.getStopLossOrderId());
        }
    }
}


