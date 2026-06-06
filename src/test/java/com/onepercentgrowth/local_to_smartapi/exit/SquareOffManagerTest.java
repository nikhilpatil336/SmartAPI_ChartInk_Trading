package com.onepercentgrowth.local_to_smartapi.exit;

import com.onepercentgrowth.local_to_smartapi.execution.OrderActionExecutor;
import com.onepercentgrowth.local_to_smartapi.helper.OrderContextFactory;
import com.onepercentgrowth.local_to_smartapi.model.OrderContext;
import com.onepercentgrowth.local_to_smartapi.model.TradeBookEntry;
import com.onepercentgrowth.local_to_smartapi.model.TradeBookResponse_v1;
import com.onepercentgrowth.local_to_smartapi.properties.ApplicationProperties;
import com.onepercentgrowth.local_to_smartapi.service.OrderBookService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SquareOffManagerTest {

    @Mock private OrderActionExecutor actionExecutor;
    @Mock private AggressiveExitManager aggressiveExitManager;
    @Mock private OrderBookService orderBookService;
    @Mock private ApplicationProperties applicationProperties;

    @InjectMocks
    private SquareOffManager manager;

    // -------------------------------------------------------
    // D1: LONG with only BUY placed, empty tradebook → cancel BUY, netQty=0, no aggressive exit
    // -------------------------------------------------------

    @Test
    void squareOff_noFills_skipsAggressiveExit() {
        OrderContext ctx = OrderContextFactory.freshLong("BUY-001", 5);

        when(actionExecutor.cancelOrder(anyString(), any(), anyString())).thenReturn(Mono.empty());
        when(applicationProperties.getExitStratWaitTimeAfterCancel()).thenReturn(0);
        when(orderBookService.fetchTradeBook_v1()).thenReturn(Mono.just(emptyTradeBook()));

        StepVerifier.create(manager.squareOff(ctx)).verifyComplete();

        verify(actionExecutor, times(1)).cancelOrder(eq("BUY-001"), any(), eq("BUY"));
        verify(aggressiveExitManager, never()).placeAggressiveExit(any(), anyInt(), any());
    }

    // -------------------------------------------------------
    // D2: LONG with all orders, tradebook shows BUY-001 filled 5 → placeAggressiveExit(5)
    // -------------------------------------------------------

    @Test
    void squareOff_withFills_triggersAggressiveExit() {
        OrderContext ctx = OrderContextFactory.longWithSellAndSlOpen(5);

        when(actionExecutor.cancelOrder(anyString(), any(), anyString())).thenReturn(Mono.empty());
        when(applicationProperties.getExitStratWaitTimeAfterCancel()).thenReturn(0);
        when(orderBookService.fetchTradeBook_v1()).thenReturn(Mono.just(tradeBookWithEntry("RELIANCE", "BUY-001", "5")));
        when(aggressiveExitManager.placeAggressiveExit(any(), anyInt(), any())).thenReturn(Mono.empty());

        StepVerifier.create(manager.squareOff(ctx)).verifyComplete();

        verify(aggressiveExitManager, times(1)).placeAggressiveExit(eq(ctx), eq(5), eq(ExitType.EOD_SQUARE_OFF));
    }

    // -------------------------------------------------------
    // D3: tradeCompleted already true → CAS fails, immediate Mono.empty(), zero side effects
    // -------------------------------------------------------

    @Test
    void squareOff_alreadyCompleted_skipsAll() {
        OrderContext ctx = OrderContextFactory.longWithSellAndSlOpen(5);
        ctx.getTradeCompleted().set(true);

        StepVerifier.create(manager.squareOff(ctx)).verifyComplete();

        verify(actionExecutor, never()).cancelOrder(any(), any(), any());
        verify(aggressiveExitManager, never()).placeAggressiveExit(any(), anyInt(), any());
    }

    // -------------------------------------------------------
    // D5: SHORT with SELL-001 filled 5 → netQty = sellFilled(5) - buyFilled(0) - slFilled(0) = 5
    //     → placeAggressiveExit(5)
    // -------------------------------------------------------

    @Test
    void squareOff_short_withFills_triggersAggressiveExit() {
        OrderContext ctx = OrderContextFactory.shortWithBuyAndSlOpen(5);

        when(actionExecutor.cancelOrder(anyString(), any(), anyString())).thenReturn(Mono.empty());
        when(applicationProperties.getExitStratWaitTimeAfterCancel()).thenReturn(0);
        when(orderBookService.fetchTradeBook_v1()).thenReturn(Mono.just(tradeBookWithEntry("RELIANCE", "SELL-001", "5")));
        when(aggressiveExitManager.placeAggressiveExit(any(), anyInt(), any())).thenReturn(Mono.empty());

        StepVerifier.create(manager.squareOff(ctx)).verifyComplete();

        verify(aggressiveExitManager, times(1)).placeAggressiveExit(eq(ctx), eq(5), eq(ExitType.EOD_SQUARE_OFF));
    }

    // -------------------------------------------------------
    // D6: SHORT with no fills → netQty = sellFilled(0) - buyFilled(0) - slFilled(0) = 0 → skip aggressive exit
    // -------------------------------------------------------

    @Test
    void squareOff_short_noFills_skipsAggressiveExit() {
        OrderContext ctx = OrderContextFactory.shortWithBuyAndSlOpen(5);

        when(actionExecutor.cancelOrder(anyString(), any(), anyString())).thenReturn(Mono.empty());
        when(applicationProperties.getExitStratWaitTimeAfterCancel()).thenReturn(0);
        when(orderBookService.fetchTradeBook_v1()).thenReturn(Mono.just(emptyTradeBook()));

        StepVerifier.create(manager.squareOff(ctx)).verifyComplete();

        verify(aggressiveExitManager, never()).placeAggressiveExit(any(), anyInt(), any());
    }

    // -------------------------------------------------------
    // D4: two calls to squareOff() — CAS is eager, second call sees tradeCompleted=true immediately
    //     → placeAggressiveExit called exactly once
    // -------------------------------------------------------

    @Test
    void squareOff_concurrentCalls_placeExitOnce() {
        OrderContext ctx = OrderContextFactory.longWithSellAndSlOpen(5);

        when(actionExecutor.cancelOrder(anyString(), any(), anyString())).thenReturn(Mono.empty());
        when(applicationProperties.getExitStratWaitTimeAfterCancel()).thenReturn(0);
        when(orderBookService.fetchTradeBook_v1()).thenReturn(Mono.just(tradeBookWithEntry("RELIANCE", "BUY-001", "5")));
        when(aggressiveExitManager.placeAggressiveExit(any(), anyInt(), any())).thenReturn(Mono.empty());

        Mono<Void> first = manager.squareOff(ctx);   // CAS(false→true) succeeds, full chain built
        Mono<Void> second = manager.squareOff(ctx);  // CAS fails, Mono.empty() returned

        StepVerifier.create(first).verifyComplete();
        StepVerifier.create(second).verifyComplete();

        verify(aggressiveExitManager, times(1)).placeAggressiveExit(any(), anyInt(), any());
    }

    private TradeBookResponse_v1 emptyTradeBook() {
        TradeBookResponse_v1 tb = new TradeBookResponse_v1();
        tb.setData(List.of());
        return tb;
    }

    private TradeBookResponse_v1 tradeBookWithEntry(String symbol, String orderId, String fillSize) {
        TradeBookEntry entry = new TradeBookEntry();
        entry.setTradingsymbol(symbol);
        entry.setOrderid(orderId);
        entry.setFillsize(fillSize);
        TradeBookResponse_v1 tb = new TradeBookResponse_v1();
        tb.setData(List.of(entry));
        return tb;
    }
}
