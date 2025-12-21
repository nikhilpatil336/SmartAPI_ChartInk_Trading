package com.onepercentgrowth.local_to_smartapi.storage;

import com.onepercentgrowth.local_to_smartapi.model.LoginResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Service
public class TokenStorageService {

    private static final Logger log = LoggerFactory.getLogger(TokenStorageService.class);

    @Value("${myapp.token.file-path}")
    private String TOKEN_FILE;

    private String jwtToken;
    private String refreshToken;
    private String feedToken;
    private long expiresAtEpoch;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public synchronized void storeTokens(LoginResponse.Data data) {
        this.jwtToken = data.getJwtToken();
        this.refreshToken = data.getRefreshToken();
        this.feedToken = data.getFeedToken();

        long expiryEpoch = computeEodEpoch();
        saveTokensToFile(expiryEpoch);
    }

    private long computeEodEpoch() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        ZonedDateTime eod = now.toLocalDate().atTime(23, 59, 59).atZone(ZoneId.systemDefault());
        return eod.toInstant().toEpochMilli();
    }

    public synchronized String getJwtToken() { return jwtToken; }
    public synchronized String getRefreshToken() { return refreshToken; }
    public synchronized String getFeedToken() { return feedToken; }

    /** -------------------------
     *   SAVE TOKENS TO FILE
     * ------------------------- */
//    private void saveTokensToFile() {
//        try {
//
//            File file = new File(TOKEN_FILE);
//
//            file.getParentFile().mkdirs();
//
//            TokenFileModel model = new TokenFileModel(jwtToken, refreshToken, feedToken);
//            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, model);
//            log.info("Tokens written successfully to {}", TOKEN_FILE);
//        } catch (Exception e) {
//            log.error("Failed to write tokens file: {}", e.getMessage(), e);
//        }
//    }

    private void saveTokensToFile(long expiryEpoch) {
        try {
            File file = new File(TOKEN_FILE);
            file.getParentFile().mkdirs();

            TokenFileModel model = new TokenFileModel(jwtToken, refreshToken, feedToken, expiryEpoch);
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, model);

            log.info("Tokens written successfully to {}", TOKEN_FILE);
        } catch (Exception e) {
            log.error("Failed to write tokens file: {}", e.getMessage(), e);
        }
    }

    /** -------------------------
     *   LOAD TOKENS FROM FILE
     * ------------------------- */
//    public synchronized void loadTokensFromFile() {
//        try {
//
//            String filePath = TOKEN_FILE;
//            File file = new File(filePath);
//
//            if (!file.exists()) {
//                log.warn("Token file not found. Will generate new tokens on next login.");
//                return;
//            }
//
//            TokenFileModel model = objectMapper.readValue(file, TokenFileModel.class);
//
//            this.jwtToken = model.jwtToken();
//            this.refreshToken = model.refreshToken();
//            this.feedToken = model.feedToken();
//
//            log.info("Tokens loaded successfully from {}", TOKEN_FILE);
//        } catch (Exception e) {
//            log.error("Failed to load tokens from file: {}", e.getMessage(), e);
//        }
//    }
    public synchronized void loadTokensFromFile() {
        try {
            File file = new File(TOKEN_FILE);
            if (!file.exists()) {
                log.warn("Token file not found.");
                return;
            }

            TokenFileModel model = objectMapper.readValue(file, TokenFileModel.class);

            this.jwtToken = model.jwtToken();
            this.refreshToken = model.refreshToken();
            this.feedToken = model.feedToken();
            this.expiresAtEpoch = model.expiresAtEpoch();

            log.info("Tokens loaded successfully from {}", TOKEN_FILE);
        } catch (Exception e) {
            log.error("Failed to load tokens from file: {}", e.getMessage(), e);
        }
    }

    public boolean isTokenExpired() {
        long now = System.currentTimeMillis();
        return now >= expiresAtEpoch;
    }


    /** -------------------------
     *   INTERNAL TOKEN MODEL
     * ------------------------- */
    private record TokenFileModel(
            String jwtToken,
            String refreshToken,
            String feedToken,
            long expiresAtEpoch
    ) {}
}
