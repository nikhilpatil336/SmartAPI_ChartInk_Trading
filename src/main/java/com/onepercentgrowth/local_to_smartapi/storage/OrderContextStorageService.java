package com.onepercentgrowth.local_to_smartapi.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onepercentgrowth.local_to_smartapi.model.OrderContext;
import com.onepercentgrowth.local_to_smartapi.model.OrderContextFileModel;
import com.onepercentgrowth.local_to_smartapi.properties.ApplicationProperties;
import com.onepercentgrowth.local_to_smartapi.registry.OrderRegistry;
import com.onepercentgrowth.local_to_smartapi.utility.OrderContextMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.*;

@Service
public class OrderContextStorageService {

    private static final Logger log = LoggerFactory.getLogger(OrderContextStorageService.class);

//    private static final String BASE_DIR = "data/order-context/";

    private final ObjectMapper mapper = new ObjectMapper();
    private final OrderRegistry registry;
    private final ApplicationProperties applicationProperties;

    private volatile int lastHash = 0;

    public OrderContextStorageService(OrderRegistry registry, ApplicationProperties applicationProperties) {
        this.registry = registry;
        this.applicationProperties = applicationProperties;
    }

    /* ================= SAVE ================= */

//    public synchronized void saveToday() {
//        try {
//            File file = getTodayFile();
//            file.getParentFile().mkdirs();
//
//            List<OrderContextFileModel> data =
//                    registry.getAllBuyContexts()
//                            .map(OrderContextMapper::toFileModel)
//                            .collectList()
//                            .block();
//
//            mapper.writerWithDefaultPrettyPrinter().writeValue(file, data);
//
//            log.info("OrderContext saved: {}", file.getAbsolutePath());
//
//        } catch (Exception e) {
//            log.error("Failed to save OrderContext", e);
//        }
//    }

    public synchronized void saveToday() {
        try {
            File file = getTodayFile();
            file.getParentFile().mkdirs();

            // 🔥 Step 1: Fetch both lists
            List<OrderContext> buyList =
                    registry.getAllBuyContexts().collectList().block();

            List<OrderContext> sellList =
                    registry.getAllSellContexts().collectList().block();

            // 🔥 Step 2: Deduplicate using BUSINESS KEY
            Map<String, OrderContext> uniqueMap = new HashMap<>();

            // Use PRIMARY order ID (VERY IMPORTANT)
            for (OrderContext ctx : buyList) {
                uniqueMap.put(getPrimaryId(ctx), ctx);
            }

            for (OrderContext ctx : sellList) {
                uniqueMap.put(getPrimaryId(ctx), ctx);
            }

            List<OrderContext> uniqueContexts =
                    new ArrayList<>(uniqueMap.values());

            if (uniqueContexts.isEmpty()) {
                log.debug("No OrderContexts to persist");
                return;
            }

            // 🔥 Step 3: Convert
            List<OrderContextFileModel> data =
                    uniqueContexts.stream()
                            .map(OrderContextMapper::toFileModel)
                            .toList();

            // 🔥 Step 4: Lightweight hash (FAST + SAFE)
//            int currentHash = data.stream()
//                    .flatMap(ctx -> java.util.stream.Stream.of(
//                            ctx.buyOrderId(),
//                            ctx.sellOrderId(),
//                            ctx.stopLossOrderId()
//                    ))
//                    .filter(java.util.Objects::nonNull)
//                    .mapToInt(String::hashCode)
//                    .sum();

            String json = mapper.writeValueAsString(data);
            int currentHash = json.hashCode();

            if (currentHash == lastHash) {
                log.debug("No changes → skipping save");
                return;
            }

            lastHash = currentHash;

            // 🔥 Step 5: Atomic write (IMPORTANT)
            File temp = new File(file.getAbsolutePath() + ".tmp");

            mapper.writerWithDefaultPrettyPrinter().writeValue(temp, data);

            // Atomic replace (cross-platform safe)
            Files.move(
                    temp.toPath(),
                    file.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE // may fallback automatically if not supported
            );

            log.info("OrderContext saved: {}", file.getAbsolutePath());

        } catch (Exception e) {
            log.error("Failed to save OrderContext | error: {}", e.getMessage());
        }
    }

    /* ================= LOAD ================= */

    public synchronized void loadToday() {
        try {
            File file = getTodayFile();

            if (!file.exists()) {
                log.warn("No OrderContext file for today. Starting fresh.");
                return;
            }

            List<OrderContextFileModel> data =
                    mapper.readValue(
                            file,
                            mapper.getTypeFactory().constructCollectionType(List.class, OrderContextFileModel.class)
                    );

            data.forEach(model -> {
                var ctx = OrderContextMapper.toDomain(model);
                registry.register(ctx);

                if (ctx.getSellOrderId() != null) registry.registerSell(ctx);
                if (ctx.getStopLossOrderId() != null) registry.registerStopLoss(ctx);
            });

            log.info("OrderContext loaded successfully from {}", file.getAbsolutePath());

        } catch (Exception e) {
            log.error("Failed to load OrderContext | error: {}", e.getMessage());
        }
    }

    public synchronized void loadLatest() {
        try {
            File file = getLatestAvailableFile();

            if (file == null || !file.exists()) {
                log.warn("No OrderContext file found. Starting fresh.");
                return;
            }

            List<OrderContextFileModel> data =
                    mapper.readValue(
                            file,
                            mapper.getTypeFactory()
                                    .constructCollectionType(List.class, OrderContextFileModel.class)
                    );

            data.forEach(model -> {
                var ctx = OrderContextMapper.toDomain(model);

                registry.register(ctx);

                if (ctx.getSellOrderId() != null) registry.registerSell(ctx);
                if (ctx.getStopLossOrderId() != null) registry.registerStopLoss(ctx);
            });

            log.info("OrderContext loaded from {}", file.getAbsolutePath());

        } catch (Exception e) {
            log.error("Failed to load OrderContext | error: {}", e.getMessage());
        }
    }

    /* ================= UTIL ================= */

    private File getTodayFile() {
        LocalDate today = LocalDate.now();
        return new File(applicationProperties.getOrderContextFilePath() + today + ".json");
    }

    private File getLatestAvailableFile() {
        try {
            File baseDir = new File(applicationProperties.getOrderContextFilePath());
            if (!baseDir.exists()) return null;

            // Get all JSON files
            File[] files = baseDir.listFiles((dir, name) -> name.endsWith(".json"));

            if (files == null || files.length == 0) return null;

            // Sort by date descending (latest first)
            return Arrays.stream(files)
                    .sorted((f1, f2) -> f2.getName().compareTo(f1.getName()))
                    .findFirst()
                    .orElse(null);

        } catch (Exception e) {
            log.error("Failed to get latest OrderContext file | error: {}", e.getMessage());
            return null;
        }
    }

    private String getPrimaryId(OrderContext ctx) {
        if (ctx.getBuyOrderId() != null) return ctx.getBuyOrderId();
        if (ctx.getSellOrderId() != null) return ctx.getSellOrderId();
        if (ctx.getStopLossOrderId() != null) return ctx.getStopLossOrderId();
        return UUID.randomUUID().toString(); // fallback (rare)
    }
}