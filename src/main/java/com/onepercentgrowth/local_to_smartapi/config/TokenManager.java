package com.onepercentgrowth.local_to_smartapi.config;

import com.onepercentgrowth.local_to_smartapi.model.LoginRequest;
import com.onepercentgrowth.local_to_smartapi.properties.AngelApiProperties;
//import com.onepercentgrowth.local_to_smartapi.scheduler.TokenScheduler;
import com.onepercentgrowth.local_to_smartapi.service.LoginService;
import com.onepercentgrowth.local_to_smartapi.storage.TokenStorageService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

//@Service
//public class TokenManagerService {
//
//    private static final Logger log = LoggerFactory.getLogger(TokenManagerService.class);
//
//    @Value("${myapp.sl_orderstore.file-path}")
//    private String slOrderBaseDir;
//
//    @Autowired
//    private TokenStorageService tokenStorageService;
//
//    @Autowired
//    private LoginService loginService;
//
//    @Autowired
//    private ScripMasterStorageService scripMasterStorageService;
//
//    @Autowired
//    private ScripMasterService scripMasterService;
//
//    @Autowired
//    private SlOrderStore slOrderStore;
//
//    @Autowired
//    @Lazy
//    private OrderStatusWebSocketService orderStatusWebSocketService;
//
//
//    @PostConstruct
//    public void init() {
//        tokenStorageService.loadTokensFromFile();
//
//        if (isTokenExpired()) {
//            try {
//                refreshTokens().block();
//            } catch (Exception e) {
//                log.error("Token refresh at startup failed: {}", e.getMessage());
//            }
//        }
//
//        scripMasterStorageService.loadFromFile();
//
//        if (scripMasterStorageService.getCachedRawList() != null &&
//                scripMasterStorageService.isFileFromToday()) {
//
//            log.info("✔ Loading today's cached ScripMaster");
//            scripMasterService.setRawScripList(scripMasterStorageService.getCachedRawList());
//
//        } else {
//            log.info("⚠ Cached ScripMaster old/missing. Fetching from API...");
//            refreshScripMasterList().block();
//        }
//
////        Path storeFile =
////                Paths.get("sl-orders-" + LocalDate.now() + ".json");
////
////        if (Files.exists(storeFile)) {
////            try {
////                byte[] bytes = Files.readAllBytes(storeFile);
////                Map<String, SlOrderMeta> loaded =
////                        new ObjectMapper().readValue(
////                                bytes,
////                                new TypeReference<>() {}
////                        );
////                slOrderStore.getMap().putAll(loaded);
////            } catch (Exception e) {
////                throw new IllegalStateException("Failed to load SL store", e);
////            }
////        }
//
////        SL_ORDER_FILE = SL_ORDER_FILE + "sl-orders-" + LocalDate.now() + ".json";
//
////        if (SL_ORDER_FILE == null || SL_ORDER_FILE.isBlank()) {
////            throw new IllegalStateException(
////                    "Property 'myapp.sl_orderstore.file-path' is NOT set"
////            );
////        }
////
////        try {
////            Path dir = Paths.get(SL_ORDER_FILE);
////            Files.createDirectories(dir);
////
////            SL_ORDER_FILE = String.valueOf(dir.resolve(
////                    "sl-orders-" + LocalDate.now() + ".json"
////            ));
////
////            System.out.println("✔ SL Order Store initialized at: " + SL_ORDER_FILE);
////
////        } catch (Exception e) {
////            throw new IllegalStateException(
////                    "Failed to initialize SL order store at: " + SL_ORDER_FILE, e
////            );
////        }
//
//        slOrderStore.init(slOrderBaseDir);
//        slOrderStore.loadFromFile();
//
//        try {
//            log.info("🚀 Starting Order Status WebSocket at startup");
//            orderStatusWebSocketService.start();
//        } catch (Exception e) {
//            log.error("❌ Failed to start Order Status WebSocket", e);
//        }
//    }
//
//    public synchronized String getValidJwtToken() {
//        if (isTokenExpired()) {
//            refreshTokens();
//        }
//        return tokenStorageService.getJwtToken();
//    }
//
//    private boolean isTokenExpired() {
//        return tokenStorageService.isTokenExpired();
//    }
//
//    private Mono<Void> refreshTokens() {
//        return loginService
//                .loginWithTotp(new LoginRequest("AACA450749", "6200"))
//                .flatMap(resp -> {
//
//                    if (resp == null) {
//                        return Mono.error(new RuntimeException("Login failed: Null response"));
//                    }
//
//                    if (resp.getData() == null) {
//                        return Mono.error(new RuntimeException(
//                                "Login failed: " + resp.getMessage()
//                        ));
//                    }
//
//                    tokenStorageService.storeTokens(resp.getData());
//                    return Mono.empty();
//                })
//                .doOnError(err -> log.error("Token refresh failed: {}", err.getMessage())).then();
//    }
//
//    private Mono<Void> refreshScripMasterList() {
//        return scripMasterService
//                .downloadRawScripMaster()
//                .flatMap(rawList -> {
//
//                    if (rawList == null) {
//                        return Mono.error(new RuntimeException("ScripMaster API returned null"));
//                    }
//
//                    scripMasterStorageService.saveRawScripMaster(rawList);
//                    scripMasterService.setRawScripList(rawList);
//
//                    return Mono.empty();
//                })
//                .doOnError(err -> log.error("ScripMaster refresh failed: {}", err.getMessage()))
//                .then();
//    }
//}

@Service
@EnableScheduling
public class TokenManager {

    private static final Logger log =
            LoggerFactory.getLogger(TokenManager.class);

    private final TokenStorageService tokenStorageService;
    private final LoginService loginService;
    private final AngelApiProperties angelApiProperties;
//    private final TokenScheduler tokenScheduler;

    public TokenManager(
            TokenStorageService tokenStorageService,
            LoginService loginService,
            AngelApiProperties angelApiProperties
//            TokenScheduler tokenScheduler
    ) {
        this.tokenStorageService = tokenStorageService;
        this.loginService = loginService;
        this.angelApiProperties = angelApiProperties;
//        this.tokenScheduler = tokenScheduler;
    }

//    @PostConstruct
//    public void init() {
//        tokenStorageService.loadTokensFromFile();
//
//        if (isTokenExpired()) {
//            refreshTokens().block();
//        }
//    }

    @PostConstruct
    public void init() {
        tokenStorageService.loadTokensFromFile();

        if (isTokenExpired()) {
            refreshTokens()
                    .subscribeOn(Schedulers.boundedElastic())
                    .subscribe();
        }
    }

    public synchronized String getValidJwtToken() {
        if (isTokenExpired()) {
            refreshTokens().block();
        }
        return tokenStorageService.getJwtToken();
    }

    public Mono<String> getValidJwtTokenAsync() {

        if (!isTokenExpired()) {
            return Mono.just(tokenStorageService.getJwtToken());
        }

        return refreshTokens()
                .then(Mono.fromSupplier(tokenStorageService::getJwtToken));
    }

    private boolean isTokenExpired() {
        return tokenStorageService.isTokenExpired();
    }

    public Mono<Void> refreshTokens() {
        return loginService
                .loginWithTotp(new LoginRequest(angelApiProperties.getClientId(), angelApiProperties.getPassword()))
                .doOnNext(resp -> tokenStorageService.storeTokens(resp.getData()))
                .then();
    }

    @Scheduled(fixedDelay = 15 * 60 * 1000)
    public void refreshIfNeeded() {
        if (isTokenExpiredSoon()) {
            refreshTokens()
                    .doOnError(e -> log.error("Scheduled token refresh failed", e))
                    .subscribe();
        }
    }

    private boolean isTokenExpiredSoon() {
        return tokenStorageService.willExpireIn(Duration.ofMinutes(5));
    }

}