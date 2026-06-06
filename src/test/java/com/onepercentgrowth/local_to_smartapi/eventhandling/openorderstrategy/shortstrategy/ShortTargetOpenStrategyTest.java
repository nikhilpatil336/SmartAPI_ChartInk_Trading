package com.onepercentgrowth.local_to_smartapi.eventhandling.openorderstrategy.shortstrategy;

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
class ShortTargetOpenStrategyTest {

    @Mock private OrderExecutionService executionService;
    @Mock private TokenManager tokenManager;
    @Mock private BalanceService balanceService;
    @Mock private LeverageService leverageService;
    @Mock private ApplicationProperties properties;
    @Mock private OrderActionExecutor orderActionExecutor;
    @Mock private AggressiveExitManager aggressiveExitManager;

    @InjectMocks
    private ShortTargetOpenStrategy strategy;

    // -------------------------------------------------------
    // SC1: partial BUY target fill (delta=2, qty=5) → remainingQty=3 → modifySL called, no SELL cancel
    // -------------------------------------------------------

    @Test
    void partialBuy_modifiesSlQty() {
        OrderContext ctx = OrderContextFactory.shortReadyForTargetOpenFill(5);
        OrderStatusResponse response = OrderStatusResponseFactory.buyOpen("BUY-001", 2, "99.00");

        when(tokenManager.getValidJwtToken()).thenReturn("test-jwt");
        when(executionService.modifyStopLossOrder(any(), any(), anyInt(), anyDouble(), anyDouble(), any(), any(), any()))
                .thenReturn(Mono.just(fakeResponse()));
        when(orderActionExecutor.safeModifyOrReplace(any(), any())).thenReturn(Mono.empty());
        when(leverageService.get(anyString())).thenReturn(new LeverageInfo("RELIANCE", 5.0));
        when(balanceService.getUsableBalance()).thenReturn(new BigDecimal("10000.00"));
        when(properties.getLeverageMultiplierToUseForShort()).thenReturn(5);

        StepVerifier.create(strategy.onFilled(ctx, response)).verifyComplete();

        verify(orderActionExecutor, times(1)).safeModifyOrReplace(any(), any());
        verify(executionService, times(1)).modifyStopLossOrder(any(), any(), eq(3), anyDouble(), anyDouble(), any(), any(), any());
        verify(executionService, never()).placeCancelOrder(any(), any(), any(), any());
    }

    // -------------------------------------------------------
    // SC2: partial BUY fill + SELL entry still open → cancel SELL-001 first, then modifySL
    // -------------------------------------------------------

    @Test
    void partialBuy_withSellOpen_cancelsSellFirst() {
        OrderContext ctx = OrderContextFactory.shortReadyForTargetOpenFill(5);
        ctx.setSellOpen(true);
        OrderStatusResponse response = OrderStatusResponseFactory.buyOpen("BUY-001", 2, "99.00");

        when(tokenManager.getValidJwtToken()).thenReturn("test-jwt");
        when(executionService.placeCancelOrder(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Mono.just(fakeResponse()));
        when(executionService.modifyStopLossOrder(any(), any(), anyInt(), anyDouble(), anyDouble(), any(), any(), any()))
                .thenReturn(Mono.just(fakeResponse()));
        when(orderActionExecutor.safeModifyOrReplace(any(), any())).thenReturn(Mono.empty());
        when(leverageService.get(anyString())).thenReturn(new LeverageInfo("RELIANCE", 5.0));
        when(balanceService.getUsableBalance()).thenReturn(new BigDecimal("10000.00"));
        when(properties.getLeverageMultiplierToUseForShort()).thenReturn(5);

        StepVerifier.create(strategy.onFilled(ctx, response)).verifyComplete();

        verify(executionService, times(1)).placeCancelOrder(eq("SELL-001"), anyString(), any(), anyString());
        verify(orderActionExecutor, times(1)).safeModifyOrReplace(any(), any());
        verify(executionService, times(1)).modifyStopLossOrder(any(), any(), eq(3), anyDouble(), anyDouble(), any(), any(), any());
    }

    // -------------------------------------------------------
    // SC3: stale BUY event (delta=0) → no SL modify, no SELL cancel
    // -------------------------------------------------------

    @Test
    void staleBuyEvent_deltaZero_noAction() {
        OrderContext ctx = OrderContextFactory.shortReadyForTargetOpenFill(5);
        ctx.getLastBuyFilledQty().set(2);
        OrderStatusResponse response = OrderStatusResponseFactory.buyOpen("BUY-001", 2, "99.00");

        StepVerifier.create(strategy.onFilled(ctx, response)).verifyComplete();

        verify(orderActionExecutor, never()).safeModifyOrReplace(any(), any());
        verify(executionService, never()).placeCancelOrder(any(), any(), any(), any());
    }

    // -------------------------------------------------------
    // SC4: full BUY target fill (delta=5, qty=5) → remainingQty=0 → no SL modify, no SELL cancel
    // -------------------------------------------------------

    @Test
    void fullBuy_remainingQtyZero_noSlModify() {
        OrderContext ctx = OrderContextFactory.shortReadyForTargetOpenFill(5);
        OrderStatusResponse response = OrderStatusResponseFactory.buyOpen("BUY-001", 5, "99.00");

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
