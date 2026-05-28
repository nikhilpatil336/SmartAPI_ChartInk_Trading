package com.onepercentgrowth.local_to_smartapi.analytics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EODAnalyticsScheduler {

    private static final Logger log = LoggerFactory.getLogger(EODAnalyticsScheduler.class);

    private final AlertAnalyticsService alertAnalyticsService;

    public EODAnalyticsScheduler(AlertAnalyticsService alertAnalyticsService) {
        this.alertAnalyticsService = alertAnalyticsService;
    }

    @Scheduled(cron = "${myapp.eod-analytics-cron}", zone = "${myapp.squareoff-zone}")
    public void runEodAnalytics() {
        log.info("EOD Analytics scheduler triggered");
        try {
            alertAnalyticsService.runForToday();
        } catch (Exception e) {
            log.error("EOD Analytics failed: {}", e.getMessage(), e);
        }
    }
}
