package com.onepercentgrowth.local_to_smartapi.service;

import com.onepercentgrowth.local_to_smartapi.model.RmsData;
import org.springframework.stereotype.Service;

@Service
public class OrderValidationService {

    public void validateToken(String jwt) {
        if (jwt == null || jwt.isBlank()) {
            throw new IllegalStateException("JWT token missing");
        }
    }

    public void validateSymbol(String symbolToken, String stockName) {
        if (symbolToken == null || symbolToken.isBlank()) {
            throw new IllegalStateException(
                    "Trading symbol not found for " + stockName
            );
        }
    }

    public void validateRms(RmsData rms) {
        if (rms == null) {
            throw new IllegalStateException("RMS data not available");
        }
    }

    // ✅ ADD THIS
    public void validateBalance(double usableCash, double minAllowed) {
        if (usableCash <= minAllowed) {
            throw new IllegalStateException(
                    "Insufficient balance. Usable=" + usableCash
                            + ", MinRequired=" + minAllowed
            );
        }
    }
}
