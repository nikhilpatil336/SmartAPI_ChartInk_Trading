package com.onepercentgrowth.local_to_smartapi.eventhandling.openorderstrategy.longstrategy;

import com.onepercentgrowth.local_to_smartapi.config.TokenManager;
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
class StopLossOpenStrategyTest {

    @Mock private OrderExecutionService executionService;
    @Mock private TokenManager tokenManager;
    @Mock private BalanceService balanceService;
    @Mock private LeverageService leverageService;
    @Mock private ApplicationProperties properties;

    @InjectMocks
    private StopLossOpenStrategy strategy;

    // -------------------------------------------------------
    // C4: partial SL fill (delta=2, remainingQty starts at 5) → stopLossRemainingQty(2)=3
    //     → modifySellOrder called with qty=3, no BUY cancel
    // -------------------------------------------------------

    @Test
    void partialSl_modifiesSellQty() {
        OrderContext ctx = OrderContextFactory.longReadyForSlOpenFill(5);
        OrderStatusResponse response = OrderStatusResponseFactory.shortEntryOpen("SL-001", 2, "99.00");

        when(tokenManager.getValidJwtToken()).thenReturn("test-jwt");
        when(executionService.modifySellOrder(any(), any(), anyInt(), anyDouble(), any(), any()))
                .thenReturn(Mono.just(fakeResponse()));
        when(leverageService.get(anyString())).thenReturn(new LeverageInfo("RELIANCE", 5.0));
        when(balanceService.getUsableBalance()).thenReturn(new BigDecimal("10000.00"));
        when(properties.getLeverageMultiplierToUseForLong()).thenReturn(5);

        StepVerifier.create(strategy.onFilled(ctx, response)).verifyComplete();

        verify(executionService, times(1)).modifySellOrder(any(), any(), eq(3), anyDouble(), any(), any());
        verify(executionService, never()).placeCancelOrder(any(), any(), any(), any());
    }

    // -------------------------------------------------------
    // C5: partial SL fill + BUY still open → cancel BUY first, then modifySellOrder
    // -------------------------------------------------------

    @Test
    void partialSl_withBuyOpen_cancelsBuyFirst() {
        OrderContext ctx = OrderContextFactory.longReadyForSlOpenFill(5);
        ctx.setBuyOpen(true);
        OrderStatusResponse response = OrderStatusResponseFactory.shortEntryOpen("SL-001", 2, "99.00");

        when(tokenManager.getValidJwtToken()).thenReturn("test-jwt");
        when(executionService.placeCancelOrder(anyString(), any(), anyString(), anyString()))
                .thenReturn(Mono.just(fakeResponse()));
        when(executionService.modifySellOrder(any(), any(), anyInt(), anyDouble(), any(), any()))
                .thenReturn(Mono.just(fakeResponse()));
        when(leverageService.get(anyString())).thenReturn(new LeverageInfo("RELIANCE", 5.0));
        when(balanceService.getUsableBalance()).thenReturn(new BigDecimal("10000.00"));
        when(properties.getLeverageMultiplierToUseForLong()).thenReturn(5);

        StepVerifier.create(strategy.onFilled(ctx, response)).verifyComplete();

        verify(executionService, times(1)).placeCancelOrder(eq("BUY-001"), any(), any(), eq("BUY"));
        verify(executionService, times(1)).modifySellOrder(any(), any(), eq(3), anyDouble(), any(), any());
    }

    // -------------------------------------------------------
    // C6: stale SL event (delta=0) → no SELL modify, no BUY cancel
    // -------------------------------------------------------

    @Test
    void staleSlEvent_deltaZero_noAction() {
        OrderContext ctx = OrderContextFactory.longReadyForSlOpenFill(5);
        ctx.getLastStoplossFilledQty().set(2);
        OrderStatusResponse response = OrderStatusResponseFactory.shortEntryOpen("SL-001", 2, "99.00");

        StepVerifier.create(strategy.onFilled(ctx, response)).verifyComplete();

        verify(executionService, never()).modifySellOrder(any(), any(), anyInt(), anyDouble(), any(), any());
        verify(executionService, never()).placeCancelOrder(any(), any(), any(), any());
    }

    // -------------------------------------------------------
    // C7: full SL fill (delta=5, qty=5) → remainingQty=0 → no SELL modify, no BUY cancel
    // -------------------------------------------------------

    @Test
    void fullSlFill_remainingQtyZero_noSellModify() {
        OrderContext ctx = OrderContextFactory.longReadyForSlOpenFill(5);
        OrderStatusResponse response = OrderStatusResponseFactory.shortEntryOpen("SL-001", 5, "99.00");

        StepVerifier.create(strategy.onFilled(ctx, response)).verifyComplete();

        verify(executionService, never()).modifySellOrder(any(), any(), anyInt(), anyDouble(), any(), any());
        verify(executionService, never()).placeCancelOrder(any(), any(), any(), any());
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
