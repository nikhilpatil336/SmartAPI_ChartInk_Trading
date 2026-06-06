package com.onepercentgrowth.local_to_smartapi.eventhandling.openorderstrategy.shortstrategy;

import com.onepercentgrowth.local_to_smartapi.config.TokenManager;
import com.onepercentgrowth.local_to_smartapi.helper.OrderContextFactory;
import com.onepercentgrowth.local_to_smartapi.helper.OrderStatusResponseFactory;
import com.onepercentgrowth.local_to_smartapi.model.LeverageInfo;
import com.onepercentgrowth.local_to_smartapi.model.OrderContext;
import com.onepercentgrowth.local_to_smartapi.model.OrderResponse;
import com.onepercentgrowth.local_to_smartapi.model.StopLossPrice;
import com.onepercentgrowth.local_to_smartapi.properties.ApplicationProperties;
import com.onepercentgrowth.local_to_smartapi.registry.OrderRegistry;
import com.onepercentgrowth.local_to_smartapi.service.BalanceService;
import com.onepercentgrowth.local_to_smartapi.service.LeverageService;
import com.onepercentgrowth.local_to_smartapi.service.OrderCalculationService;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShortEntryOpenStrategyTest {

    @Mock private OrderExecutionService executionService;
    @Mock private OrderCalculationService calculationService;
    @Mock private TokenManager tokenManager;
    @Mock private OrderRegistry orderRegistry;
    @Mock private BalanceService balanceService;
    @Mock private LeverageService leverageService;
    @Mock private ApplicationProperties applicationProperties;

    @InjectMocks
    private ShortEntryOpenStrategy strategy;

    // -------------------------------------------------------
    // Scenario 1: partial fill (delta > 0) — BUY target + SL must be placed
    // -------------------------------------------------------

    @Test
    void partialFill_placesBuyTargetAndSL() {
        OrderContext ctx = OrderContextFactory.freshShort("SELL-001", 5);
        OrderStatusResponse response = OrderStatusResponseFactory.shortEntryOpen("SELL-001", 3, "100.00");

        when(calculationService.calculateSellProfitPrice(any(), anyString()))
                .thenReturn(new BigDecimal("99.00"));
        when(calculationService.calculateShortStopLossPrice(any(), any(), any(), anyString()))
                .thenReturn(new StopLossPrice(new BigDecimal("101.00"), new BigDecimal("101.10")));
        when(tokenManager.getValidJwtToken()).thenReturn("test-jwt");
        when(applicationProperties.getTradingStoplossPercent()).thenReturn(0.5);
        when(applicationProperties.getTradingStoplossBufferPercent()).thenReturn(0.1);
        when(leverageService.get(anyString())).thenReturn(new LeverageInfo("RELIANCE", 5.0));
        when(executionService.placeBuyOrder(anyString(), anyString(), anyInt(), anyString(), anyString()))
                .thenReturn(Mono.just(fakeResponse("BUY-001")));
        when(executionService.placeStopLossOrder(anyString(), anyString(), anyInt(), anyDouble(), anyDouble(), anyString(), anyString()))
                .thenReturn(Mono.just(fakeResponse("SL-001")));

        StepVerifier.create(strategy.onFilled(ctx, response))
                .verifyComplete();

        verify(executionService, times(1))
                .placeBuyOrder(anyString(), anyString(), anyInt(), anyString(), anyString());
        verify(executionService, times(1))
                .placeStopLossOrder(anyString(), anyString(), anyInt(), anyDouble(), anyDouble(), anyString(), anyString());
        assertThat(ctx.getBuyOrderId()).isEqualTo("BUY-001");
        assertThat(ctx.getStopLossOrderId()).isEqualTo("SL-001");
    }

    // -------------------------------------------------------
    // Scenario 2: open event with filledshares=0
    //   delta=0 → returns empty BEFORE CAS is claimed
    //   CAS must stay free so the subsequent complete event can claim it
    // -------------------------------------------------------

    @Test
    void openEventWithZeroFilled_doesNotClaimCasOrPlaceOrders() {
        OrderContext ctx = OrderContextFactory.freshShort("SELL-001", 5);
        OrderStatusResponse response = OrderStatusResponseFactory.shortEntryOpen("SELL-001", 0, "100.00");

        StepVerifier.create(strategy.onFilled(ctx, response))
                .verifyComplete();

        verify(executionService, never()).placeBuyOrder(any(), any(), anyInt(), any(), any());
        verify(executionService, never()).placeStopLossOrder(any(), any(), anyInt(), anyDouble(), anyDouble(), any(), any());
        assertThat(ctx.tryBeginEntryOrders())
                .as("CAS must still be free so the complete event can claim it")
                .isTrue();
    }

    // -------------------------------------------------------
    // Scenario 3: CAS already claimed (race — another event arrived first)
    //   no duplicate BUY target or SL orders placed
    // -------------------------------------------------------

    @Test
    void casAlreadyClaimed_doesNotPlaceDuplicateOrders() {
        OrderContext ctx = OrderContextFactory.shortWithCasAlreadyClaimed("SELL-001", 5);
        OrderStatusResponse response = OrderStatusResponseFactory.shortEntryOpen("SELL-001", 3, "100.00");

        StepVerifier.create(strategy.onFilled(ctx, response))
                .verifyComplete();

        verify(executionService, never()).placeBuyOrder(any(), any(), anyInt(), any(), any());
        verify(executionService, never()).placeStopLossOrder(any(), any(), anyInt(), anyDouble(), anyDouble(), any(), any());
    }

    // -------------------------------------------------------
    // Scenario 4: stale WS event — broker replays the same filledshares value
    //   lastSellFilledQty already = filledshares → delta=0 → CAS must stay free
    // -------------------------------------------------------

    @Test
    void staleWsEvent_deltaZero_doesNotClaimCasOrPlaceOrders() {
        OrderContext ctx = OrderContextFactory.freshShort("SELL-001", 5);
        ctx.getLastSellFilledQty().set(3); // already processed qty=3
        OrderStatusResponse response = OrderStatusResponseFactory.shortEntryOpen("SELL-001", 3, "100.00");

        StepVerifier.create(strategy.onFilled(ctx, response))
                .verifyComplete();

        verify(executionService, never()).placeBuyOrder(any(), any(), anyInt(), any(), any());
        verify(executionService, never()).placeStopLossOrder(any(), any(), anyInt(), anyDouble(), anyDouble(), any(), any());
        assertThat(ctx.tryBeginEntryOrders())
                .as("CAS must still be free — stale event must not claim it")
                .isTrue();
    }

    private OrderResponse fakeResponse(String orderId) {
        OrderResponse r = new OrderResponse();
        r.setStatus(true);
        OrderResponse.Data d = new OrderResponse.Data();
        d.setOrderid(orderId);
        r.setData(d);
        return r;
    }
}
