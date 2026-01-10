//package com.onepercentgrowth.local_to_smartapi.scheduler;
//
//import com.onepercentgrowth.local_to_smartapi.properties.ApplicationProperties;
//import com.onepercentgrowth.local_to_smartapi.service.RmsService;
//import org.springframework.scheduling.annotation.EnableScheduling;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//
//@Component
//@EnableScheduling
//public class RmsRefreshScheduler {
//
//    private final RmsService rmsService;
//    private final ApplicationProperties applicationProperties;
//
//    public RmsRefreshScheduler(RmsService rmsService,
//                               ApplicationProperties applicationProperties) {
//        this.rmsService = rmsService;
//        this.applicationProperties = applicationProperties;
//    }
//
//    @Scheduled(fixedDelayString =
//            "#{${myapp.rms-refresh-interval-minutes} * 60 * 1000}")
//    public void refreshRmsIfEnabled() {
//
//        if (!applicationProperties.isRmsAutoRefreshEnable()) {
//            return;
//        }
//
//        rmsService.refreshNow().subscribe();
//    }
//}
//
