package com.onepercentgrowth.local_to_smartapi.websocket;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

//@JsonIgnoreProperties(ignoreUnknown = true)
//public class OrderStatusResponse {
//
////    @JsonProperty("user-id")
//    private String userId;
//
////    @JsonProperty("status-code")
//    private String statusCode;
//
////    @JsonProperty("order-status")
//    private String orderStatus;
//
////    @JsonProperty("error-message")
//    private String errorMessage;
//
////    @JsonProperty("orderStatusData")
//    private OrderStatusData orderStatusData;
//
//    public String getUserId() {
//        return userId;
//    }
//
//    public void setUserId(String userId) {
//        this.userId = userId;
//    }
//
//    public String getStatusCode() {
//        return statusCode;
//    }
//
//    public void setStatusCode(String statusCode) {
//        this.statusCode = statusCode;
//    }
//
//    public String getOrderStatus() {
//        return orderStatus;
//    }
//
//    public void setOrderStatus(String orderStatus) {
//        this.orderStatus = orderStatus;
//    }
//
//    public String getErrorMessage() {
//        return errorMessage;
//    }
//
//    public void setErrorMessage(String errorMessage) {
//        this.errorMessage = errorMessage;
//    }
//
//    public OrderStatusData getOrderStatusData() {
//        return orderStatusData;
//    }
//
//    public void setOrderStatusData(OrderStatusData orderStatusData) {
//        this.orderStatusData = orderStatusData;
//    }
//
//    @Override
//    public String toString() {
//        return "OrderStatusResponse{" +
//                "userId='" + userId + '\'' +
//                ", statusCode='" + statusCode + '\'' +
//                ", orderStatus='" + orderStatus + '\'' +
//                ", errorMessage='" + errorMessage + '\'' +
//                ", orderStatusData=" + orderStatusData +
//                '}';
//    }
//}

@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderStatusResponse {

    @JsonProperty("user-id")
    private String userId;

    @JsonProperty("status-code")
    private String statusCode;

    @JsonProperty("order-status")
    private String orderStatus;

    @JsonProperty("error-message")
    private String errorMessage;

    @JsonProperty("orderData")
    private OrderStatusData orderStatusData;

    // getters & setters

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public OrderStatusData getOrderStatusData() {
        return orderStatusData;
    }

    public void setOrderStatusData(OrderStatusData orderStatusData) {
        this.orderStatusData = orderStatusData;
    }

    @Override
    public String toString() {
        return "OrderStatusResponse{" +
                "userId='" + userId + '\'' +
                ", statusCode='" + statusCode + '\'' +
                ", orderStatus='" + orderStatus + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                ", orderStatusData=" + orderStatusData +
                '}';
    }
}