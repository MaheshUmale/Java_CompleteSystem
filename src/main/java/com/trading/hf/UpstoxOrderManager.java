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

    public UpstoxOrderManager(String accessToken) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        OAuth oAuth = (OAuth) defaultClient.getAuthentication("OAUTH2");
        oAuth.setAccessToken(accessToken);
        this.orderApi = new OrderApi(defaultClient);
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

            return orderApi.placeOrder(body, "2.0");
        } catch (ApiException e) {
            System.err.println("Error placing order: " + e.getResponseBody());
            return null;
        }
    }
}
