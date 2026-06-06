package com.onepercentgrowth.local_to_smartapi.eventhandling.openorderstrategy.longstrategy;

import com.onepercentgrowth.local_to_smartapi.config.TokenManager;
import com.onepercentgrowth.local_to_smartapi.execution.OrderActionExecutor;
import com.onepercentgrowth.local_to_smartapi.exit.AggressiveExitManager;
import com.onepercentgrowth.local_to_smartapi.helper.OrderContextFactory;
import com.onepercentgrowth.local_to_smartapi.helper.OrderStatusResponseFactory;
import com.onepercentgrowth.local_to_smartapi.model.LeverageInfo;
import com.onepercentgrowth.local_to_smartapi.model.OrderContext;
import com.onepercentgrowth.local_to_smartapi.model.OrderResponse;
import com.onepercentgrowth.local_to_smartapi.properties.ApplicationProperties;
import com.onepercentgrowth.local_to_smartapi.service.BalanceService;
import com.onepercentgrowth.local_to_smartapi.service.LeverageService;
import com.onepercentgrowth.local_to_smartapi.service.OrderExecutionService;
import com.onepercentgrowth.local_to_smartapi.websocket.OrderStatusResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SellOpenStrategyTest {

    @Mock private OrderExecutionService executionService;
    @Mock private TokenManager tokenManager;
    @Mock private BalanceService balanceService;
    @Mock private LeverageService leverageService;
    @Mock private ApplicationProperties properties;
    @Mock private OrderActionExecutor orderActionExecutor;
    @Mock private AggressiveExitManager aggressiveExitManager;

    @InjectMocks
    private SellOpenStrategy strategy;

    // -------------------------------------------------------
    // C1: partial SELL (delta=2, qty=5) → remainingQty=3 → modifyStopLoss called, no BUY cancel
    // -------------------------------------------------------

    @Test
    void partialSell_modifiesSlQty() {
        OrderContext ctx = OrderContextFactory.longReadyForSellOpenFill(5);
        OrderStatusResponse response = OrderStatusResponseFactory.sellOpen("SELL-001", 2, "101.00");

        when(tokenManager.getValidJwtToken()).thenReturn("test-jwt");
        when(executionService.modifyStopLossOrder(any(), any(), anyInt(), anyDouble(), anyDouble(), any(), any(), any()))
                .thenReturn(Mono.just(fakeResponse()));
        when(orderActionExecutor.safeModifyOrReplace(any(), any())).thenReturn(Mono.empty());
        when(leverageService.get(anyString())).thenReturn(new LeverageInfo("RELIANCE", 5.0));
        when(balanceService.getUsableBalance()).thenReturn(new BigDecimal("10000.00"));
        when(properties.getLeverageMultiplierToUseForLong()).thenReturn(5);

        StepVerifier.create(strategy.onFilled(ctx, response)).verifyComplete();

        verify(orderActionExecutor, times(1)).safeModifyOrReplace(any(), any());
        verify(executionService, times(1)).modifyStopLossOrder(any(), any(), eq(3), anyDouble(), anyDouble(), any(), any(), any());
        verify(executionService, never()).placeCancelOrder(any(), any(), any(), any());
    }

    // -------------------------------------------------------
    // C2: partial SELL + BUY still open → cancel BUY first, then modifyStopLoss
    // -------------------------------------------------------

    @Test
    void partialSell_withBuyOpen_cancelsBuyFirst() {
        OrderContext ctx = OrderContextFactory.longReadyForSellOpenFill(5);
        ctx.setBuyOpen(true);
        OrderStatusResponse response = OrderStatusResponseFactory.sellOpen("SELL-001", 2, "101.00");

        when(tokenManager.getValidJwtToken()).thenReturn("test-jwt");
        when(executionService.placeCancelOrder(anyString(), any(), anyString(), anyString()))
                .thenReturn(Mono.just(fakeResponse()));
        when(executionService.modifyStopLossOrder(any(), any(), anyInt(), anyDouble(), anyDouble(), any(), any(), any()))
                .thenReturn(Mono.just(fakeResponse()));
        when(orderActionExecutor.safeModifyOrReplace(any(), any())).thenReturn(Mono.empty());
        when(leverageService.get(anyString())).thenReturn(new LeverageInfo("RELIANCE", 5.0));
        when(balanceService.getUsableBalance()).thenReturn(new BigDecimal("10000.00"));
        when(properties.getLeverageMultiplierToUseForLong()).thenReturn(5);

        StepVerifier.create(strategy.onFilled(ctx, response)).verifyComplete();

        verify(executionService, times(1)).placeCancelOrder(eq("BUY-001"), any(), any(), eq("BUY"));
        verify(orderActionExecutor, times(1)).safeModifyOrReplace(any(), any());
        verify(executionService, times(1)).modifyStopLossOrder(any(), any(), eq(3), anyDouble(), anyDouble(), any(), any(), any());
    }

    // -------------------------------------------------------
    // C3: stale SELL event (delta=0) → no SL modify, no BUY cancel
    // -------------------------------------------------------

    @Test
    void staleSellEvent_deltaZero_noAction() {
        OrderContext ctx = OrderContextFactory.longReadyForSellOpenFill(5);
        ctx.getLastSellFilledQty().set(2);
        OrderStatusResponse response = OrderStatusResponseFactory.sellOpen("SELL-001", 2, "101.00");

        StepVerifier.create(strategy.onFilled(ctx, response)).verifyComplete();

        verify(orderActionExecutor, never()).safeModifyOrReplace(any(), any());
        verify(executionService, never()).placeCancelOrder(any(), any(), any(), any());
    }

    // -------------------------------------------------------
    // C4: full SELL (delta=5, qty=5) → remainingQty=0 → no SL modify, no BUY cancel
    // -------------------------------------------------------

    @Test
    void fullSell_remainingQtyZero_noSlModify() {
        OrderContext ctx = OrderContextFactory.longReadyForSellOpenFill(5);
        OrderStatusResponse response = OrderStatusResponseFactory.sellOpen("SELL-001", 5, "101.00");

        StepVerifier.create(strategy.onFilled(ctx, response)).verifyComplete();

        verify(orderActionExecutor, never()).safeModifyOrReplace(any(), any());
        verify(executionService, never()).modifyStopLossOrder(any(), any(), anyInt(), anyDouble(), anyDouble(), any(), any(), any());
    }

    private OrderResponse fakeResponse() {
        OrderResponse r = new OrderResponse();
        r.setStatus(true);
        OrderResponse.Data d = new OrderResponse.Data();
        d.setOrderid("OK");
        r.setData(d);
        return r;
    }
}
