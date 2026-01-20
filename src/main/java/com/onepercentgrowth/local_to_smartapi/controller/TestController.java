package com.onepercentgrowth.local_to_smartapi.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onepercentgrowth.local_to_smartapi.config.TokenManager;
import com.onepercentgrowth.local_to_smartapi.eventhandling.OrderEventQueue;
import com.onepercentgrowth.local_to_smartapi.model.OrderContext;
import com.onepercentgrowth.local_to_smartapi.properties.ApplicationProperties;
import com.onepercentgrowth.local_to_smartapi.registry.OrderRegistry;
import com.onepercentgrowth.local_to_smartapi.service.OrderExecutionService;
import com.onepercentgrowth.local_to_smartapi.websocket.OrderStatusResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

@RestController
public class TestController {

    private static final Logger log = LoggerFactory.getLogger(TestController.class);

    @Autowired
    private OrderEventQueue orderEventQueue;
    @Autowired
    private OrderRegistry orderRegistry;
    @Autowired
    private OrderExecutionService orderExecutionService;
    @Autowired
    private TokenManager tokenManager;
    @Autowired
    private ApplicationProperties applicationProperties;

    private final ObjectMapper mapper = new ObjectMapper();

    @PostMapping("/test")
    public void test(@RequestBody String payload) throws JsonProcessingException {
        OrderStatusResponse response =
                mapper.readValue(payload, OrderStatusResponse.class);

        orderEventQueue.publish(response);
    }

    @PostMapping("/testSellandStoploss")
    public void testSellandStoploss(@RequestBody OrderStatusResponse response) {

        if (response.getOrderStatusData() == null) {
            throw new IllegalArgumentException("orderStatusData missing in payload");
        }

        Optional<OrderContext> existingCtx =
                orderRegistry.getByAnyOrderId(response.getOrderStatusData().getOrderid());

        if (existingCtx.isPresent()) {
            // BUY already processed → do nothing / reuse
            log.info("OrderContext already exists for orderId={}", response.getOrderStatusData().getOrderid());
            orderEventQueue.publish(response);
            return;
        }

        if(response.getOrderStatusData().getTransactiontype().equalsIgnoreCase("BUY"))
        {
            OrderContext orderContext = new OrderContext(
                    "260106000810894",   // buyOrderId
                    "260106000810939",   // sellOrderId
                    "260106000811022",   // stopLossOrderId
                    response.getOrderStatusData().getTradingsymbol(),         // tradingSymbol
                    response.getOrderStatusData().getSymboltoken(),              // symbolToken
                    Integer.parseInt(response.getOrderStatusData().getQuantity()),                   // quantity
                    "NORMAL",            // sellVariety
                    "STOPLOSS"           // stopLossVariety
            );

            orderRegistry.registerBuy(orderContext);
        }

        // Publish incoming event
        orderEventQueue.publish(response);
    }

    @GetMapping("/modify")
    public void modify()
    {
        String tradingSymbol = "TATASTEEL-EQ";
        String symbolToken = "3499";
        int quantity = 10;
        double price = 100.0;

        orderExecutionService
                .placeStopLossOrder(
                        tradingSymbol,
                        symbolToken,
                        quantity,
                        10,
                        price,
                        tokenManager.getValidJwtToken()
                )
                .flatMap(orderResponse ->
                        orderExecutionService.modifyStopLossOrder(
                                tradingSymbol,
                                symbolToken,
                                quantity+10,
                                0,
                                price, // new price (can be different)
                                orderResponse.getData().getOrderid(),
                                tokenManager.getValidJwtToken()
                        )
                )
                .doOnSuccess(resp ->
                        log.info("SELL order modified successfully orderId={}",
                                resp.getData().getOrderid())
                )
                .doOnError(err ->
                        log.error("SELL place/modify flow failed", err)
                )
                .onErrorResume(err -> Mono.empty())
                .subscribe();
    }


    @PostMapping(
            value = "/test/webhookRequest",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<String> webhookRequest(
            @RequestBody Map<String, Object> payload
    ) {

        try {
            log.info("📩 Chartink Webhook Received");
            log.info("Payload: {}", payload);

            log.info("Stocks: {}", payload.get("stocks"));
            log.info("Trigger Prices: {}", payload.get("trigger_prices"));
            log.info("Triggered At: {}", payload.get("triggered_at"));

            saveToExcel(payload);

            return ResponseEntity.ok("Webhook received successfully");

        } catch (Exception ex) {
            log.error("❌ Error processing webhook", ex);
            return ResponseEntity
                    .internalServerError()
                    .body("Error processing webhook");
        }
    }


    // YOUR EXISTING METHOD (NO CHANGE)
    private void saveToExcel(Map<String, Object> body) {
        try {
            Path path = Path.of(applicationProperties.getExcelToSaveAlerts());

            Workbook workbook;
            Sheet sheet;

            if (Files.notExists(path)) {
                workbook = new XSSFWorkbook();
                sheet = workbook.createSheet("chartinkAllAlerts");

                Row header = sheet.createRow(0);
                header.createCell(0).setCellValue("stocks");
                header.createCell(1).setCellValue("trigger_prices");
                header.createCell(2).setCellValue("triggered_at");

            } else {
                FileInputStream fis = new FileInputStream(applicationProperties.getExcelToSaveAlerts());
                workbook = WorkbookFactory.create(fis);
                sheet = workbook.getSheetAt(0);
                fis.close();
            }

            int lastRow = sheet.getLastRowNum();
            Row row = sheet.createRow(lastRow + 1);

            row.createCell(0).setCellValue(String.valueOf(body.get("stocks")));
            row.createCell(1).setCellValue(String.valueOf(body.get("trigger_prices")));
            row.createCell(2).setCellValue(String.valueOf(body.get("triggered_at")));

            FileOutputStream fos = new FileOutputStream(applicationProperties.getExcelToSaveAlerts());
            workbook.write(fos);
            fos.close();
            workbook.close();

            log.info("✅ Alert appended to Excel");

        } catch (Exception e) {
            throw new RuntimeException("Excel write failed", e);
        }
    }

}
