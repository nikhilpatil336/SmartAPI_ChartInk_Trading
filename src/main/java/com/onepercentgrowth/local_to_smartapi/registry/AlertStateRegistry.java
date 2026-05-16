package com.onepercentgrowth.local_to_smartapi.registry;

import com.onepercentgrowth.local_to_smartapi.model.FirstAlertData;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AlertStateRegistry {

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
}
