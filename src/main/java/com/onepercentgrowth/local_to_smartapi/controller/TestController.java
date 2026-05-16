package com.onepercentgrowth.local_to_smartapi.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onepercentgrowth.local_to_smartapi.config.TokenManager;
import com.onepercentgrowth.local_to_smartapi.enums.PositionSide;
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
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
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

    @PostMapping("/testShortSellAndStoploss")
    public void testShortSellAndStoploss(@RequestBody OrderStatusResponse response) {

        if (response.getOrderStatusData() == null) {
            throw new IllegalArgumentException("orderStatusData missing in payload");
        }

        Optional<OrderContext> existingCtx =
                orderRegistry.getByAnyOrderId(
                        response.getOrderStatusData().getOrderid()
                );

        if (existingCtx.isPresent()) {
            log.info("SHORT OrderContext already exists for orderId={}",
                    response.getOrderStatusData().getOrderid());
            orderEventQueue.publish(response);
            return;
        }

        // SHORT ENTRY = SELL
        if (response.getOrderStatusData()
                .getTransactiontype()
                .equalsIgnoreCase("SELL")) {

            OrderContext orderContext = new OrderContext(
                    "260106000810894",   // buyOrderId (TARGET BUY)
                    "260106000810939",   // sellOrderId (ENTRY SELL)
                    "260106000811022",   // stopLossOrderId (BUY SL)
                    response.getOrderStatusData().getTradingsymbol(),
                    response.getOrderStatusData().getSymboltoken(),
                    Integer.parseInt(response.getOrderStatusData().getQuantity()),
                    "NORMAL",      // sellVariety (entry)
                    "STOPLOSS"     // stopLossVariety
            );

            // For SHORT, ENTRY is SELL
            orderRegistry.registerSell(orderContext);
        }

        orderEventQueue.publish(response);
    }

    @GetMapping("/modify/{orderType}")
    public void modify(@PathVariable String orderType)
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
                        tokenManager.getValidJwtToken(),
                        orderType
                )
                .flatMap(orderResponse ->
                        orderExecutionService.modifyStopLossOrder(
                                tradingSymbol,
                                symbolToken,
                                quantity+10,
                                0,
                                price, // new price (can be different)
                                orderResponse.getData().getOrderid(),
                                tokenManager.getValidJwtToken(),
                                orderType
                        )
                )
                .doOnSuccess(resp ->
                        log.info("SELL order modified successfully orderId={}",
                                resp.getData().getOrderid())
                )
                .doOnError(err ->
                        log.error("SELL place/modify flow failed | error: {}", err.getMessage())
                )
                .onErrorResume(err -> Mono.empty())
                .subscribe();
    }


    @PostMapping(
            value = "/save/webhookrequest",
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

//            String excelLocation = applicationProperties.getExcelToSaveAlerts();

            if(payload.get("scan_name").toString().equalsIgnoreCase("V1_Buy_Low_Sell_High"))
                saveToExcel(payload, applicationProperties.getGrowthAlertExcelPath());
            else if(payload.get("scan_name").toString().equalsIgnoreCase("Invert_Buy_Low_Sell_High"))
                saveToExcel(payload, applicationProperties.getShortAlertExcelPath());
            else if(payload.get("scan_name").toString().equalsIgnoreCase("Buy Low = Sell High"))
                saveToExcel(payload, applicationProperties.getExcelToSaveAlerts());

            return ResponseEntity.ok("Webhook received successfully");

        } catch (Exception ex) {
            log.error("❌ Error processing webhook | error:{}", ex.getMessage());
            return ResponseEntity
                    .internalServerError()
                    .body("Error processing webhook");
        }
    }

    @PostMapping(
            value = "/test/webhookRequestShort",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<String> webhookRequestShort(
            @RequestBody Map<String, Object> payload
    ) {

        try {
            log.info("📩 SHORT Chartink Webhook Received");
            log.info("Payload: {}", payload);

            log.info("Stocks: {}", payload.get("stocks"));
            log.info("Trigger Prices: {}", payload.get("trigger_prices"));
            log.info("Triggered At: {}", payload.get("triggered_at"));

            // For now ONLY short scan
            if (payload.get("scan_name").toString()
                    .equalsIgnoreCase("Invert_Buy_Low_Sell_High")) {

                saveToExcel(payload, applicationProperties.getShortAlertExcelPath());
            }

            return ResponseEntity.ok("Short Webhook received successfully");

        } catch (Exception ex) {
            log.error("❌ Error processing SHORT webhook | error: {}", ex.getMessage());
            return ResponseEntity
                    .internalServerError()
                    .body("Error processing SHORT webhook");
        }
    }

    private void saveToExcel(Map<String, Object> body, String excelPath) {
        try {
            Path path = Path.of(excelPath);

            // ✅ This fixes everything
            Files.createDirectories(path.getParent());

            Workbook workbook;
            Sheet sheet;

            if (Files.notExists(path)) {
                workbook = new XSSFWorkbook();
                sheet = workbook.createSheet("chartinkAllAlerts");

                Row header = sheet.createRow(0);
                header.createCell(0).setCellValue("stocks");
                header.createCell(1).setCellValue("trigger_prices");
                header.createCell(2).setCellValue("triggered_at");
                header.createCell(3).setCellValue("saved_at");
            } else {
                try (FileInputStream fis = new FileInputStream(path.toFile())) {
                    workbook = WorkbookFactory.create(fis);
                    sheet = workbook.getSheetAt(0);
                }
            }

            int lastRow = sheet.getLastRowNum();
            Row row = sheet.createRow(lastRow + 1);

            row.createCell(0).setCellValue(String.valueOf(body.get("stocks")));
            row.createCell(1).setCellValue(String.valueOf(body.get("trigger_prices")));
            row.createCell(2).setCellValue(String.valueOf(body.get("triggered_at")));

            Cell dateCell = row.createCell(3);
            dateCell.setCellValue(new Date());

            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(
                    workbook.getCreationHelper()
                            .createDataFormat()
                            .getFormat("yyyy-MM-dd HH:mm:ss")
            );
            dateCell.setCellStyle(dateStyle);

            try (FileOutputStream fos = new FileOutputStream(path.toFile())) {
                workbook.write(fos);
            }

            workbook.close();

            log.info("✅ Excel file written at {}", path);

        } catch (Exception e) {
            throw new RuntimeException("Excel write failed", e);
        }
    }
}
