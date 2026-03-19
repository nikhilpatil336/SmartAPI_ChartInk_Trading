package com.onepercentgrowth.local_to_smartapi.scheduler;

import com.onepercentgrowth.local_to_smartapi.exit.AggressiveExitManager;
import com.onepercentgrowth.local_to_smartapi.exit.SquareOffManager;
import com.onepercentgrowth.local_to_smartapi.properties.ApplicationProperties;
import com.onepercentgrowth.local_to_smartapi.registry.OrderRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class EODSquareOffScheduler {

    private static final Logger log =
            LoggerFactory.getLogger(EODSquareOffScheduler.class);

    private final SquareOffManager squareOffManager;
    private final OrderRegistry registry;

    public EODSquareOffScheduler(SquareOffManager squareOffManager,
                                 OrderRegistry registry) {
        this.squareOffManager = squareOffManager;
        this.registry = registry;
    }

//    @Scheduled(cron = "0 0 15 * * MON-FRI", zone = "Asia/Kolkata")
//    public void runSquareOff() {
//
//        registry.getAllContexts()
//                .flatMap(squareOffManager::squareOff)
//                .doOnError(e -> log.error("SquareOff error", e))
//                .subscribe();
//    }

    @Scheduled(cron = "${myapp.squareoff-cron}", zone = "${myapp.squareoff-zone}")
    public void runSquareOff() {

        registry.getAllContexts()
                .flatMap(ctx ->
                        squareOffManager.squareOff(ctx)
                                .onErrorResume(e -> {
                                    log.error("SquareOff failed for {}", ctx.getTradingSymbol(), e);
                                    return Mono.empty();
                                })
                )
                .subscribe();
    }
}