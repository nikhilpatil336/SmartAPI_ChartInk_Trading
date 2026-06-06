package com.onepercentgrowth.local_to_smartapi.storage;

import com.onepercentgrowth.local_to_smartapi.properties.ApplicationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.scheduler.Schedulers;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
public class AlertTriggerLogService {

    private static final Logger log = LoggerFactory.getLogger(AlertTriggerLogService.class);
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("EEE, MMM d, yyyy h:mm a", Locale.ENGLISH);

    private final ApplicationProperties applicationProperties;

    public AlertTriggerLogService(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    public void logAlert(String symbol, LocalDateTime triggeredAt, double firstPrice, double secondPrice) {
        String line = triggeredAt.format(FMT)
                + "|1|" + symbol
                + "|" + String.format("%.2f", firstPrice)
                + "|" + String.format("%.2f", secondPrice);

        Mono.fromRunnable(() -> writeToFile(symbol, line))
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(
                        null,
                        e -> log.error("Alert trigger log write failed for {}: {}", symbol, e.getMessage())
                );
    }

    private void writeToFile(String symbol, String line) {
        try {
            Path path = Paths.get(applicationProperties.getAlertTriggerLogPath());
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            Files.write(path, List.of(line), StandardOpenOption.APPEND, StandardOpenOption.CREATE);
            log.info("Alert trigger logged: {}", line);
        } catch (IOException e) {
            log.error("Failed to write alert trigger log for {}: {}", symbol, e.getMessage());
        }
    }
}
