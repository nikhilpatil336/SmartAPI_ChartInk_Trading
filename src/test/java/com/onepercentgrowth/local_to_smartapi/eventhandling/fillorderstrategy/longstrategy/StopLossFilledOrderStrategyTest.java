package com.onepercentgrowth.local_to_smartapi.eventhandling.fillorderstrategy.longstrategy;

import com.onepercentgrowth.local_to_smartapi.config.TokenManager;
import com.onepercentgrowth.local_to_smartapi.helper.OrderContextFactory;
import com.onepercentgrowth.local_to_smartapi.helper.OrderStatusResponseFactory;
import com.onepercentgrowth.local_to_smartapi.model.LeverageInfo;
import com.onepercentgrowth.local_to_smartapi.model.OrderContext;
import com.onepercentgrowth.local_to_smartapi.model.OrderResponse;
import com.onepercentgrowth.local_to_smartapi.properties.ApplicationProperties;
import com.onepercentgrowth.local_to_smartapi.registry.OrderRegistry;
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

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StopLossFilledOrderStrategyTest {

    @Mock private OrderExecutionService executionService;
    @Mock private TokenManager tokenManager;
    @Mock private OrderRegistry orderRegistry;
    @Mock private BalanceService balanceService;
    @Mock private LeverageService leverageService;
    @Mock private ApplicationProperties applicationProperties;

    @InjectMocks
    private StopLossFilledOrderStrategy strategy;

    // -------------------------------------------------------
    // Scenario 1: LONG SL fills → cancel target sell, mark trade complete, remove from registry
    // -------------------------------------------------------

    @Test
    void slFills_cancelsTargetSell_marksTradeComplete() {
        OrderContext ctx = OrderContextFactory.longWithSellAndSlOpen(5);
        // SELL type, orderId matches ctx.getStopLossOrderId()
        OrderStatusResponse response = OrderStatusResponseFactory.sellComplete("SL-001", 5, "99.00");

        when(tokenManager.getValidJwtToken()).thenReturn("test-jwt");
        when(leverageService.get(anyString())).thenReturn(new LeverageInfo("RELIANCE", 5.0));
        when(executionService.placeCancelOrder(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Mono.just(fakeResponse()));

        StepVerifier.create(strategy.onFilled(ctx, response))
                .verifyComplete();

        verify(executionService, times(1))
                .placeCancelOrder(eq("SELL-001"), anyString(), anyString(), anyString());
        verify(orderRegistry, times(1)).remove(ctx);
        assertThat(ctx.getTradeCompleted().get()).isTrue();
    }

    // -------------------------------------------------------
    // Scenario 2: tradeCompleted already true (duplicate WS event)
    //   → no cancel, no registry remove
    // -------------------------------------------------------

    @Test
    void tradeAlreadyCompleted_ignoresDuplicateEvent() {
        OrderContext ctx = OrderContextFactory.longWithSellAndSlOpen(5);
        ctx.getTradeCompleted().set(true);
        OrderStatusResponse response = OrderStatusResponseFactory.sellComplete("SL-001", 5, "99.00");

        StepVerifier.create(strategy.onFilled(ctx, response))
                .verifyComplete();

        verify(executionService, never()).placeCancelOrder(any(), any(), any(), any());
        verify(orderRegistry, never()).remove(any());
    }

    // -------------------------------------------------------
    // E2: SELL cancel fails after 3 retries → markInconsistent, tradeCompleted stays false, no registry.remove
    // -------------------------------------------------------

    @Test
    void cancelTargetSellFails_marksInconsistent_noTradeCompletion() {
        OrderContext ctx = OrderContextFactory.longWithSellAndSlOpen(5);
        OrderStatusResponse response = OrderStatusResponseFactory.sellComplete("SL-001", 5, "99.00");

        when(tokenManager.getValidJwtToken()).thenReturn("test-jwt");
        when(executionService.placeCancelOrder(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Mono.error(new RuntimeException("network timeout")));

        StepVerifier.withVirtualTime(() -> strategy.onFilled(ctx, response))
                .thenAwait(Duration.ofSeconds(30))
                .verifyComplete();

        assertThat(ctx.isSystemInconsistent()).isTrue();
        assertThat(ctx.getTradeCompleted().get()).isFalse();
        verify(orderRegistry, never()).remove(any());
    }

    private OrderResponse fakeResponse() {
        OrderResponse r = new OrderResponse();
        r.setStatus(true);
        OrderResponse.Data d = new OrderResponse.Data();
        d.setOrderid("CANCEL-OK");
        r.setData(d);
        return r;
    }
}
