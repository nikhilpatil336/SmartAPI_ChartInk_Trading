package com.onepercentgrowth.local_to_smartapi.execution;

import com.onepercentgrowth.local_to_smartapi.config.TokenManager;
import com.onepercentgrowth.local_to_smartapi.service.OrderExecutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.function.Supplier;

@Component
public class OrderActionExecutor {

    private static final Logger log =
            LoggerFactory.getLogger(OrderActionExecutor.class);

    private final OrderExecutionService executionService;
    private final TokenManager tokenManager;

    public OrderActionExecutor(OrderExecutionService executionService,
                               TokenManager tokenManager) {
        this.executionService = executionService;
        this.tokenManager = tokenManager;
    }

    public Mono<Void> cancelOrder(String orderId, String variety, String orderType) {
        String jwt = tokenManager.getValidJwtToken();

        return executionService.placeCancelOrder(orderId, variety, jwt, orderType)
                .retryWhen(
                        Retry.backoff(3, Duration.ofMillis(200))
                                .doBeforeRetry(rs ->
                                        log.warn("Retrying Cancle Order... attempt={}", rs.totalRetries())
                                )
                )
                .doOnSuccess(resp ->
                        log.info("Order cancelled | orderId={}", orderId))
                .doOnError(e ->
                        log.error("Cancel failed | orderId={} | error: {}", orderId, e.getMessage()))
                .then();
    }

//    public Mono<Void> safeModifyOrReplace(
//            Mono<?> modifyMono,
//            Runnable cancelAndReplaceFallback
//    ) {
//        return modifyMono
//                .doOnError(e -> {
//                    log.warn("MODIFY failed → fallback to CANCEL + PLACE", e);
//                    cancelAndReplaceFallback.run();
//                })
//                .onErrorResume(e -> Mono.empty())
//                .then();
//    }


    public Mono<Void> safeModifyOrReplace(
            Mono<?> modifyMono,
            Supplier<Mono<Void>> fallback
    ) {
        return modifyMono
                .retryWhen(
                        Retry.backoff(3, Duration.ofMillis(200))
                                .doBeforeRetry(rs ->
                                        log.warn("Retrying Modify or replace... attempt={}", rs.totalRetries())
                                )
                )
                .then()
                .onErrorResume(e -> {
                    log.warn("MODIFY failed → fallback", e);
                    return fallback.get();
                });
    }
}
