package com.onepercentgrowth.local_to_smartapi.registry;

import com.onepercentgrowth.local_to_smartapi.model.OrderContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Collection;
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

//@Component
//public class OrderRegistry {
//
//    private final ConcurrentMap<String, OrderContext> byBuyId = new ConcurrentHashMap<>();
//    private final ConcurrentMap<String, OrderContext> bySellId = new ConcurrentHashMap<>();
//    private final ConcurrentMap<String, OrderContext> bySlId = new ConcurrentHashMap<>();
//
//    // Phase 1: BUY registered
//    public void registerBuy(OrderContext ctx) {
//        byBuyId.put(ctx.getBuyOrderId(), ctx);
//    }
//
//    public void register(OrderContext ctx) {
//        byBuyId.put(ctx.getBuyOrderId(), ctx);
//        bySellId.put(ctx.getSellOrderId(), ctx);
//        bySlId.put(ctx.getStopLossOrderId(), ctx);
//    }
//
//    // Phase 2: SELL + SL registered
//    public void registerSellAndSl(OrderContext ctx) {
//        if (ctx.getSellOrderId() != null) {
//            bySellId.put(ctx.getSellOrderId(), ctx);
//        }
//        if (ctx.getStopLossOrderId() != null) {
//            bySlId.put(ctx.getStopLossOrderId(), ctx);
//        }
//    }
//
////    public void registerSell(OrderContext ctx) {
////        if (ctx.getSellOrderId() != null) {
////            bySellId.put(ctx.getSellOrderId(), ctx);
////        }
////    }
//    public void registerSell(OrderContext ctx) {
//        OrderContext existing = byBuyId.get(ctx.getBuyOrderId());
//        if (existing != null) {
//            bySellId.put(ctx.getSellOrderId(), existing);
//        }
//    }
//
//    public void registerStopLoss(OrderContext ctx) {
//        if (ctx.getStopLossOrderId() != null) {
//            bySlId.put(ctx.getStopLossOrderId(), ctx);
//        }
//    }
//
//
//    public Optional<OrderContext> getByAnyOrderId(String orderId) {
//
//        OrderContext ctx = byBuyId.get(orderId);
//        if (ctx != null) return Optional.of(ctx);
//
//        ctx = bySellId.get(orderId);
//        if (ctx != null) return Optional.of(ctx);
//
//        ctx = bySlId.get(orderId);
//        if (ctx != null) return Optional.of(ctx);
//
//        return Optional.empty();
//    }
//
//    public Optional<OrderContext> getByBuyId(String buyOrderId) {
//        return Optional.ofNullable(byBuyId.get(buyOrderId));
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
//        if (ctx.getSellOrderId() != null) {
//            bySellId.remove(ctx.getSellOrderId());
//        }
//        if (ctx.getStopLossOrderId() != null) {
//            bySlId.remove(ctx.getStopLossOrderId());
//        }
//    }
//}

//@Component
//public class OrderRegistry {
//
//    // Single source of truth
//    private final ConcurrentMap<String, OrderContext> byBuyId = new ConcurrentHashMap<>();
//
//    // Indexes (all point to SAME OrderContext)
//    private final ConcurrentMap<String, String> sellToBuy = new ConcurrentHashMap<>();
//    private final ConcurrentMap<String, String> slToBuy = new ConcurrentHashMap<>();
//
//    /* ===================== REGISTER ===================== */
//
//    public Optional<OrderContext> getByAnyOrderId(String orderId) {
//
//        OrderContext ctx = byBuyId.get(orderId);
//        if (ctx != null) return Optional.of(ctx);
//
//        ctx = byBuyId.get(sellToBuy.get(orderId));
//        if (ctx != null) return Optional.of(ctx);
//
//        ctx = byBuyId.get(slToBuy.get(orderId));
//        if (ctx != null) return Optional.of(ctx);
//
//        return Optional.empty();
//    }
//
//    public void register(OrderContext ctx) {
//        registerBuy(ctx);
//
//        if (ctx.getSellOrderId() != null) {
//            registerSell(ctx);
//        }
//
//        if (ctx.getStopLossOrderId() != null) {
//            registerStopLoss(ctx);
//        }
//    }
//
//
//    public void registerBuy(OrderContext ctx) {
//        byBuyId.put(ctx.getBuyOrderId(), ctx);
//    }
//
//    public void registerSell(OrderContext ctx) {
//        assertTradeExists(ctx);
//        sellToBuy.put(ctx.getSellOrderId(), ctx.getBuyOrderId());
//    }
//
//    public void registerStopLoss(OrderContext ctx) {
//        assertTradeExists(ctx);
//        slToBuy.put(ctx.getStopLossOrderId(), ctx.getBuyOrderId());
//    }
//
//    /* ===================== LOOKUPS ===================== */
//
//    public Optional<OrderContext> getByBuyId(String buyId) {
//        return Optional.ofNullable(byBuyId.get(buyId));
//    }
//
//    public Optional<OrderContext> getBySellId(String sellId) {
//        String buyId = sellToBuy.get(sellId);
//        return buyId == null ? Optional.empty()
//                : Optional.ofNullable(byBuyId.get(buyId));
//    }
//
//    public Optional<OrderContext> getBySlId(String slId) {
//        String buyId = slToBuy.get(slId);
//        return buyId == null ? Optional.empty()
//                : Optional.ofNullable(byBuyId.get(buyId));
//    }
//
//    /* ===================== CLEANUP ===================== */
//
//    public void remove(OrderContext ctx) {
//        byBuyId.remove(ctx.getBuyOrderId());
//        sellToBuy.values().remove(ctx.getBuyOrderId());
//        slToBuy.values().remove(ctx.getBuyOrderId());
//    }
//
//    /* ===================== ASSERTIONS ===================== */
//
//    private void assertTradeExists(OrderContext ctx) {
//        if (!byBuyId.containsKey(ctx.getBuyOrderId())) {
//            throw new IllegalStateException(
//                    "OrderContext not registered via BUY first: " + ctx.getBuyOrderId()
//            );
//        }
//    }
//}

@Component
public class OrderRegistry {

    // Single source of truth
    private final ConcurrentMap<String, OrderContext> byBuyId = new ConcurrentHashMap<>();

    // Secondary indexes → SAME OrderContext instance
    private final ConcurrentMap<String, OrderContext> bySellId = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, OrderContext> bySlId = new ConcurrentHashMap<>();

    private final ConcurrentMap<String, OrderContext> byExitId = new ConcurrentHashMap<>();

    /* ===================== REGISTER ===================== */

    public void registerBuy(OrderContext ctx) {
        byBuyId.put(ctx.getBuyOrderId(), ctx);
    }

    public void registerSell(OrderContext ctx) {
//        assertTradeExists(ctx);
        bySellId.put(ctx.getSellOrderId(), ctx);
    }

    public void registerStopLoss(OrderContext ctx) {
//        assertTradeExists(ctx);
        bySlId.put(ctx.getStopLossOrderId(), ctx);
    }

    public void register(OrderContext ctx) {
        registerBuy(ctx);

        if (ctx.getSellOrderId() != null) {
            registerSell(ctx);
        }

        if (ctx.getStopLossOrderId() != null) {
            registerStopLoss(ctx);
        }
    }

    public void registerExit(String exitOrderId, OrderContext ctx) {
        byExitId.put(exitOrderId, ctx);
    }

    /* ===================== LOOKUPS ===================== */

//    public Optional<OrderContext> getByAnyOrderId(String orderId) {
//        OrderContext ctx = byBuyId.get(orderId);
//        if (ctx != null) return Optional.of(ctx);
//
//        ctx = bySellId.get(orderId);
//        if (ctx != null) return Optional.of(ctx);
//
//        ctx = bySlId.get(orderId);
//        if (ctx != null) return Optional.of(ctx);
//
//        return Optional.empty();
//    }

    public Optional<OrderContext> getByAnyOrderId(String orderId) {

        OrderContext ctx = byBuyId.get(orderId);
        if (ctx != null) return Optional.of(ctx);

        ctx = bySellId.get(orderId);
        if (ctx != null) return Optional.of(ctx);

        ctx = bySlId.get(orderId);
        if (ctx != null) return Optional.of(ctx);

        ctx = byExitId.get(orderId);
        if (ctx != null) return Optional.of(ctx);

        return Optional.empty();
    }

    /* ===================== CLEANUP ===================== */

    public void remove(OrderContext ctx) {
        byBuyId.remove(ctx.getBuyOrderId());

        if (ctx.getSellOrderId() != null) {
            bySellId.remove(ctx.getSellOrderId());
        }

        if (ctx.getStopLossOrderId() != null) {
            bySlId.remove(ctx.getStopLossOrderId());
        }
    }

    /* ===================== ASSERTIONS ===================== */

    private void assertTradeExists(OrderContext ctx) {
        if (!byBuyId.containsKey(ctx.getBuyOrderId())) {
            throw new IllegalStateException(
                    "OrderContext not registered via BUY first: " + ctx.getBuyOrderId()
            );
        }
    }


//    public Collection<OrderContext> getAllContexts() {
//        return byBuyId.values();
//    }

    public Flux<OrderContext> getAllBuyContexts() {
        return Flux.fromIterable(byBuyId.values());
    }

    public Flux<OrderContext> getAllSellContexts() {
        return Flux.fromIterable(bySellId.values());
    }
}