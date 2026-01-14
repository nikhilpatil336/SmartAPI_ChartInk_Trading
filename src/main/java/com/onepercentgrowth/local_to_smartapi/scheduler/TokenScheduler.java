//package com.onepercentgrowth.local_to_smartapi.scheduler;
//
//import com.onepercentgrowth.local_to_smartapi.config.TokenManager;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.scheduling.annotation.EnableScheduling;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//
//@Component
//@EnableScheduling
//public class TokenScheduler {
//
//    @Autowired
//    private TokenManager tokenManager;
//
//    @Scheduled(fixedDelay = 15 * 60 * 1000)
//    public void refreshIfNeeded() {
//        if (isTokenExpiredSoon()) {
//            tokenManager.refreshTokens().subscribe();
//        }
//    }
//}
