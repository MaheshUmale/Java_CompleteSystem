package com.trading.hf;

import com.upstox.ApiClient;
import com.upstox.ApiException;
import com.upstox.Configuration;
import com.upstox.auth.OAuth;
import io.swagger.client.api.OrderApi;
import com.upstox.api.PlaceOrderRequest;
import com.upstox.api.PlaceOrderResponse;


public class UpstoxOrderManager {

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
        try {
            PlaceOrderRequest body = new PlaceOrderRequest();
            body.setInstrumentToken(instrumentKey);
            body.setQuantity(quantity);
            body.setProduct(PlaceOrderRequest.ProductEnum.D);
            body.setValidity(PlaceOrderRequest.ValidityEnum.DAY);
            body.setOrderType(orderType.equals("LIMIT") ? PlaceOrderRequest.OrderTypeEnum.LIMIT : PlaceOrderRequest.OrderTypeEnum.MARKET);
            body.setTransactionType(side.equals("BUY") ? PlaceOrderRequest.TransactionTypeEnum.BUY : PlaceOrderRequest.TransactionTypeEnum.SELL);
            body.setPrice((float)price);
            body.setDisclosedQuantity(0);
            body.setTriggerPrice(0.0f);
            body.setIsAmo(false);

            PlaceOrderResponse response = orderApi.placeOrder(body, "2.0");
            if (response != null && response.getStatus().equals("success")) {
                // TODO: Use the actual fill price from the order response
                positionManager.addPosition(instrumentKey, quantity, side, price, System.currentTimeMillis());
            }
            return response;
        } catch (ApiException e) {
            System.err.println("Error placing order: " + e.getResponseBody());
            return null;
        }
    }
}
