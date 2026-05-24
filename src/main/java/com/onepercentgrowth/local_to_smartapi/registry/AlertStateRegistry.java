package com.onepercentgrowth.local_to_smartapi.registry;

import com.onepercentgrowth.local_to_smartapi.model.FirstAlertData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AlertStateRegistry {

    private static final Logger log = LoggerFactory.getLogger(AlertStateRegistry.class);

    private final Map<String, FirstAlertData> store = new ConcurrentHashMap<>();

    public void save(String symbol, FirstAlertData data) {
        store.put(symbol, data);
    }

    public FirstAlertData get(String symbol) {
        return store.get(symbol);
    }

    public void remove(String symbol) {
        store.remove(symbol);
    }

    public void cancelFallbackIfPending(String symbol) {
        FirstAlertData data = store.get(symbol);
        if (data != null && data.getFallbackTask() != null && !data.getFallbackTask().isDone()) {
            data.getFallbackTask().cancel(false);
            log.info("Cancelled fallback task for {} (batch cleanup after order placed)", symbol);
        }
    }
}
