package com.onepercentgrowth.local_to_smartapi.config;

import com.onepercentgrowth.local_to_smartapi.model.LoginRequest;
import com.onepercentgrowth.local_to_smartapi.model.SlOrderMeta;
import com.onepercentgrowth.local_to_smartapi.service.LoginService;
import com.onepercentgrowth.local_to_smartapi.service.ScripMasterService;
import com.onepercentgrowth.local_to_smartapi.storage.ScripMasterStorageService;
import com.onepercentgrowth.local_to_smartapi.storage.SlOrderStore;
import com.onepercentgrowth.local_to_smartapi.storage.TokenStorageService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Map;

@Service
public class TokenManagerService {

    private static final Logger log = LoggerFactory.getLogger(TokenManagerService.class);

    @Value("${myapp.sl_orderstore.file-path}")
    private String slOrderBaseDir;

    @Autowired
    private TokenStorageService tokenStorageService;

    @Autowired
    private LoginService loginService;

    @Autowired
    private ScripMasterStorageService scripMasterStorageService;

    @Autowired
    private ScripMasterService scripMasterService;

    @Autowired
    private SlOrderStore slOrderStore;

    @PostConstruct
    public void init() {
        tokenStorageService.loadTokensFromFile();

        if (isTokenExpired()) {
            try {
                refreshTokens().block();
            } catch (Exception e) {
                log.error("Token refresh at startup failed: {}", e.getMessage());
            }
        }

        scripMasterStorageService.loadFromFile();

        if (scripMasterStorageService.getCachedRawList() != null &&
                scripMasterStorageService.isFileFromToday()) {

            log.info("✔ Loading today's cached ScripMaster");
            scripMasterService.setRawScripList(scripMasterStorageService.getCachedRawList());

        } else {
            log.info("⚠ Cached ScripMaster old/missing. Fetching from API...");
            refreshScripMasterList().block();
        }

//        Path storeFile =
//                Paths.get("sl-orders-" + LocalDate.now() + ".json");
//
//        if (Files.exists(storeFile)) {
//            try {
//                byte[] bytes = Files.readAllBytes(storeFile);
//                Map<String, SlOrderMeta> loaded =
//                        new ObjectMapper().readValue(
//                                bytes,
//                                new TypeReference<>() {}
//                        );
//                slOrderStore.getMap().putAll(loaded);
//            } catch (Exception e) {
//                throw new IllegalStateException("Failed to load SL store", e);
//            }
//        }

//        SL_ORDER_FILE = SL_ORDER_FILE + "sl-orders-" + LocalDate.now() + ".json";

//        if (SL_ORDER_FILE == null || SL_ORDER_FILE.isBlank()) {
//            throw new IllegalStateException(
//                    "Property 'myapp.sl_orderstore.file-path' is NOT set"
//            );
//        }
//
//        try {
//            Path dir = Paths.get(SL_ORDER_FILE);
//            Files.createDirectories(dir);
//
//            SL_ORDER_FILE = String.valueOf(dir.resolve(
//                    "sl-orders-" + LocalDate.now() + ".json"
//            ));
//
//            System.out.println("✔ SL Order Store initialized at: " + SL_ORDER_FILE);
//
//        } catch (Exception e) {
//            throw new IllegalStateException(
//                    "Failed to initialize SL order store at: " + SL_ORDER_FILE, e
//            );
//        }

        slOrderStore.init(slOrderBaseDir);
        slOrderStore.loadFromFile();

    }

    public synchronized String getValidJwtToken() {
        if (isTokenExpired()) {
            refreshTokens();
        }
        return tokenStorageService.getJwtToken();
    }

    private boolean isTokenExpired() {
        return tokenStorageService.isTokenExpired();
    }

    private Mono<Void> refreshTokens() {
        return loginService
                .loginWithTotp(new LoginRequest("AACA450749", "6200"))
                .flatMap(resp -> {

                    if (resp == null) {
                        return Mono.error(new RuntimeException("Login failed: Null response"));
                    }

                    if (resp.getData() == null) {
                        return Mono.error(new RuntimeException(
                                "Login failed: " + resp.getMessage()
                        ));
                    }

                    tokenStorageService.storeTokens(resp.getData());
                    return Mono.empty();
                })
                .doOnError(err -> log.error("Token refresh failed: {}", err.getMessage())).then();
    }

    private Mono<Void> refreshScripMasterList() {
        return scripMasterService
                .downloadRawScripMaster()
                .flatMap(rawList -> {

                    if (rawList == null) {
                        return Mono.error(new RuntimeException("ScripMaster API returned null"));
                    }

                    scripMasterStorageService.saveRawScripMaster(rawList);
                    scripMasterService.setRawScripList(rawList);

                    return Mono.empty();
                })
                .doOnError(err -> log.error("ScripMaster refresh failed: {}", err.getMessage()))
                .then();
    }
}

