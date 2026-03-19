package com.onepercentgrowth.local_to_smartapi.execution;

import com.onepercentgrowth.local_to_smartapi.config.TokenManager;
import com.onepercentgrowth.local_to_smartapi.model.OrderContext;
import com.onepercentgrowth.local_to_smartapi.service.OrderExecutionService;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class CancelReplaceExecutor {

    private final OrderExecutionService executionService;
    private final TokenManager tokenManager;
    private final OrderActionExecutor actionExecutor;

    public CancelReplaceExecutor(
            OrderExecutionService executionService,
            TokenManager tokenManager,
            OrderActionExecutor actionExecutor
    ) {
        this.executionService = executionService;
        this.tokenManager = tokenManager;
        this.actionExecutor = actionExecutor;
    }

    public Mono<Void> cancelRemainingBuy(OrderContext ctx) {
        String jwt = tokenManager.getValidJwtToken();

//        return actionExecutor.retryCancel(
//                executionService.placeCancelOrder(
//                        ctx.getBuyOrderId(),
//                        ctx.getBuyVariety(),
//                        jwt,
//                        "BUY"
//                ).then(),
//                3
//        ).doOnSuccess(v -> ctx.setBuyOpen(false));

        return actionExecutor.cancelOrder(
                ctx.getBuyOrderId(),
                ctx.getBuyVariety(),
                "BUY"
        ).doOnSuccess(v -> ctx.setBuyOpen(false));
    }
}

