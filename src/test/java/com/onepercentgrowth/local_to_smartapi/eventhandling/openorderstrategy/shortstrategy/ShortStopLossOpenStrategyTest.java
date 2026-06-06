package com.onepercentgrowth.local_to_smartapi.eventhandling.openorderstrategy.shortstrategy;

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
class ShortStopLossOpenStrategyTest {

    @Mock private OrderExecutionService executionService;
    @Mock private TokenManager tokenManager;
    @Mock private BalanceService balanceService;
    @Mock private LeverageService leverageService;
    @Mock private ApplicationProperties properties;

    @InjectMocks
    private ShortStopLossOpenStrategy strategy;

    // -------------------------------------------------------
    // SC4: partial SL fill (delta=2, remainingQty starts at 5) → stopLossRemainingQty(2)=3
    //      → modifyBuyOrder called with qty=3, no SELL cancel
    // -------------------------------------------------------

    @Test
    void partialSl_modifiesBuyQty() {
        OrderContext ctx = OrderContextFactory.shortReadyForSlOpenFill(5);
        OrderStatusResponse response = OrderStatusResponseFactory.buyOpen("SL-001", 2, "101.00");

        when(tokenManager.getValidJwtToken()).thenReturn("test-jwt");
        when(executionService.modifyBuyOrder(any(), any(), anyInt(), anyString(), any(), any()))
                .thenReturn(Mono.just(fakeResponse()));
        when(leverageService.get(anyString())).thenReturn(new LeverageInfo("RELIANCE", 5.0));
        when(balanceService.getUsableBalance()).thenReturn(new BigDecimal("10000.00"));
        when(properties.getLeverageMultiplierToUseForShort()).thenReturn(5);

        StepVerifier.create(strategy.onFilled(ctx, response)).verifyComplete();

        verify(executionService, times(1)).modifyBuyOrder(any(), any(), eq(3), anyString(), any(), any());
        verify(executionService, never()).placeCancelOrder(any(), any(), any(), any());
    }

    // -------------------------------------------------------
    // SC5: partial SL fill + SELL entry still open → cancel SELL-001 first, then modifyBuyOrder
    // -------------------------------------------------------

    @Test
    void partialSl_withSellOpen_cancelsSellFirst() {
        OrderContext ctx = OrderContextFactory.shortReadyForSlOpenFill(5);
        ctx.setSellOpen(true);
        OrderStatusResponse response = OrderStatusResponseFactory.buyOpen("SL-001", 2, "101.00");

        when(tokenManager.getValidJwtToken()).thenReturn("test-jwt");
        when(executionService.placeCancelOrder(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Mono.just(fakeResponse()));
        when(executionService.modifyBuyOrder(any(), any(), anyInt(), anyString(), any(), any()))
                .thenReturn(Mono.just(fakeResponse()));
        when(leverageService.get(anyString())).thenReturn(new LeverageInfo("RELIANCE", 5.0));
        when(balanceService.getUsableBalance()).thenReturn(new BigDecimal("10000.00"));
        when(properties.getLeverageMultiplierToUseForShort()).thenReturn(5);

        StepVerifier.create(strategy.onFilled(ctx, response)).verifyComplete();

        verify(executionService, times(1)).placeCancelOrder(eq("SELL-001"), anyString(), anyString(), anyString());
        verify(executionService, times(1)).modifyBuyOrder(any(), any(), eq(3), anyString(), any(), any());
    }

    // -------------------------------------------------------
    // SC6: stale SL event (delta=0) → no BUY modify, no SELL cancel
    // -------------------------------------------------------

    @Test
    void staleSlEvent_deltaZero_noAction() {
        OrderContext ctx = OrderContextFactory.shortReadyForSlOpenFill(5);
        ctx.getLastStoplossFilledQty().set(2);
        OrderStatusResponse response = OrderStatusResponseFactory.buyOpen("SL-001", 2, "101.00");

        StepVerifier.create(strategy.onFilled(ctx, response)).verifyComplete();

        verify(executionService, never()).modifyBuyOrder(any(), any(), anyInt(), anyString(), any(), any());
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
