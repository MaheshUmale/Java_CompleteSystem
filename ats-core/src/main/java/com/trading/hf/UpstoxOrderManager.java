package com.trading.hf;

import com.upstox.ApiClient;
import com.upstox.ApiException;
import com.upstox.Configuration;
import com.upstox.auth.OAuth;
import io.swagger.client.api.OrderApi;
import com.upstox.api.PlaceOrderRequest;
import com.upstox.api.PlaceOrderResponse;
import com.upstox.api.ModifyOrderRequest;
import com.upstox.api.ModifyOrderResponse;
import com.upstox.api.CancelOrderResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class UpstoxOrderManager {

    private static final Logger logger = LoggerFactory.getLogger(UpstoxOrderManager.class);
    private final OrderApi orderApi;
    private final PositionManager positionManager;

    public UpstoxOrderManager(String accessToken, PositionManager positionManager) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        OAuth oAuth = (OAuth) defaultClient.getAuthentication("OAUTH2");
        oAuth.setAccessToken(accessToken);
        this.orderApi = new OrderApi(defaultClient);
        this.positionManager = positionManager;
    }

    public PlaceOrderResponse placeOrder(String instrumentKey, int quantity, String side, String orderType, double price) {
        if (orderType == null) {
            logger.error("Order type cannot be null for instrumentKey: {}", instrumentKey);
            return null;
        }

        try {
            PlaceOrderRequest body = new PlaceOrderRequest();
            body.setInstrumentToken(instrumentKey);
            body.setQuantity(quantity);
            body.setProduct(PlaceOrderRequest.ProductEnum.D);
            body.setValidity(PlaceOrderRequest.ValidityEnum.DAY);

            switch (orderType.toUpperCase()) {
                case "LIMIT":
                    body.setOrderType(PlaceOrderRequest.OrderTypeEnum.LIMIT);
                    break;
                case "MARKET":
                    body.setOrderType(PlaceOrderRequest.OrderTypeEnum.MARKET);
                    break;
                default:
                    logger.error("Unsupported order type: {}. Order rejected.", orderType);
                    return null;
            }
            body.setTransactionType(side.equals("BUY") ? PlaceOrderRequest.TransactionTypeEnum.BUY : PlaceOrderRequest.TransactionTypeEnum.SELL);
            body.setPrice((float)price);
            body.setDisclosedQuantity(0);
            body.setTriggerPrice(0.0f);
            body.setIsAmo(false);

            PlaceOrderResponse response = orderApi.placeOrder(body, "2.0");
            if (response != null && "success".equals(response.getStatus())) {
                // CRITICAL: The PlaceOrderResponse does not contain the fill price.
                // We are assuming the fill price is the same as the requested price for LIMIT orders.
                // For MARKET orders, this is incorrect and will lead to inaccurate P&L.
                if ("LIMIT".equals(orderType.toUpperCase())) {
                    positionManager.addPosition(instrumentKey, quantity, side, price, System.currentTimeMillis());
                } else {
                    logger.warn("Cannot accurately track position for MARKET order {}. Position not added. A fill-tracking service is required.", response.getData().getOrderId());
                }
            }
            return response;
        } catch (ApiException e) {
            logger.error("Error placing order: {}", e.getResponseBody(), e);
            return null;
        }
    }

    public ModifyOrderResponse modifyOrder(String orderId, int quantity, String orderType, double price, String validity, double triggerPrice) {
        if (orderType == null) {
            logger.error("Order type cannot be null for orderId: {}", orderId);
            return null;
        }
        if (validity == null) {
            logger.error("Validity cannot be null for orderId: {}", orderId);
            return null;
        }

        try {
            ModifyOrderRequest body = new ModifyOrderRequest();
            body.setQuantity(quantity);
            try {
                body.setValidity(ModifyOrderRequest.ValidityEnum.fromValue(validity));
            } catch (IllegalArgumentException e) {
                logger.error("Invalid validity value provided: {}", validity, e);
                return null;
            }
            switch (orderType.toUpperCase()) {
                case "LIMIT":
                    body.setOrderType(ModifyOrderRequest.OrderTypeEnum.LIMIT);
                    break;
                case "MARKET":
                    body.setOrderType(ModifyOrderRequest.OrderTypeEnum.MARKET);
                    break;
                default:
                    logger.error("Unsupported order type for modification: {}. Order modification rejected.", orderType);
                    return null;
            }
            body.setPrice((float)price);
            body.setTriggerPrice((float)triggerPrice);

            return orderApi.modifyOrder(body, orderId, "2.0");
        } catch (ApiException e) {
            logger.error("Error modifying order: {}", e.getResponseBody(), e);
            return null;
        }
    }

    public CancelOrderResponse cancelOrder(String orderId) {
        try {
            return orderApi.cancelOrder(orderId, "2.0");
        } catch (ApiException e) {
            logger.error("Error canceling order: {}", e.getResponseBody(), e);
            return null;
        }
    }
}
