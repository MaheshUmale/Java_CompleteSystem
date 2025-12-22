package com.trading.hf;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import com.lmax.disruptor.RingBuffer;
import com.upstox.marketdatafeederv3udapi.rpc.proto.MarketDataFeedV3;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class UpstoxMarketDataStreamer {

    private static final Logger logger = LoggerFactory.getLogger(UpstoxMarketDataStreamer.class);
    private final String accessToken;
    private final RingBuffer<MarketEvent> marketEventRingBuffer;
    private final RingBuffer<RawFeedEvent> rawFeedRingBuffer;
    private final boolean persistenceEnabled;
    private Set<String> instrumentKeys;

    private WebSocketClient webSocketClient;
    private DynamicStrikeSubscriber strikeSubscriber;

    public UpstoxMarketDataStreamer(
            String accessToken,
            RingBuffer<MarketEvent> marketEventRingBuffer,
            RingBuffer<RawFeedEvent> rawFeedRingBuffer,
            Set<String> instrumentKeys
    ) {
        this.accessToken = accessToken;
        this.marketEventRingBuffer = marketEventRingBuffer;
        this.rawFeedRingBuffer = rawFeedRingBuffer;
        this.instrumentKeys = instrumentKeys;
        this.persistenceEnabled = ConfigLoader.getBooleanProperty("database.persistence.enabled", false);
    }

    public void connect() {
        try {
            URI serverUri = getWebsocketUrl(this.accessToken);
            this.webSocketClient = createWebSocketClient(serverUri);
            this.webSocketClient.connect();
        } catch (IOException | InterruptedException e) {
            logger.error("Failed to connect to Upstox WebSocket", e);
            Thread.currentThread().interrupt();
        }
    }

    private URI getWebsocketUrl(String token) throws IOException, InterruptedException {
        String url = "https://api.upstox.com/v3/feed/market-data-feed/authorize";
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url))
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + token)
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        Gson gson = new Gson();
        WebSocketAuthResponse authResponse = gson.fromJson(response.body(), WebSocketAuthResponse.class);
        return URI.create(authResponse.getData().getAuthorizedRedirectUri());
    }

    private WebSocketClient createWebSocketClient(URI serverUri) {
        return new WebSocketClient(serverUri) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                logger.info("Upstox WebSocket connection opened");
                sendSubscriptionRequest(this, instrumentKeys);
            }

            @Override
            public void onMessage(String message) {
                logger.info("Received Text Message: {}", message);
            }

            @Override
            public void onMessage(ByteBuffer bytes) {
                handleBinaryMessage(bytes);
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                logger.info("Upstox WebSocket connection closed by {}. Code: {}, Reason: {}", (remote ? "remote peer" : "us"), code, reason);
            }

            @Override
            public void onError(Exception ex) {
                logger.error("Upstox WebSocket error", ex);
            }
        };
    }

    private void handleBinaryMessage(ByteBuffer bytes) {
        try {
            byte[] data = new byte[bytes.remaining()];
            bytes.get(data);
            MarketDataFeedV3.FeedResponse feedResponse = MarketDataFeedV3.FeedResponse.parseFrom(data);
            feedResponse.getFeedsMap().forEach((instrumentToken, feed) -> {
                if (strikeSubscriber != null && "NSE_INDEX|Nifty 50".equals(instrumentToken)) {
                    if (feed.hasLtpc()) {
                        strikeSubscriber.onNiftySpotPrice(feed.getLtpc().getLtp());
                    }
                }

                publishMarketEvent(instrumentToken, feed);

                if (persistenceEnabled) {
                    publishRawFeedUpdate(instrumentToken, feed);
                }
            });

        } catch (Exception e) {
            logger.error("Error parsing binary message from Upstox", e);
        }
    }

    private void sendSubscriptionRequest(WebSocketClient client, Set<String> instruments) {
        JsonObject requestObject = constructSubscriptionRequest("sub", instruments);
        byte[] binaryData = requestObject.toString().getBytes(StandardCharsets.UTF_8);
        logger.info("Sending Subscription Request: {}", requestObject);
        client.send(binaryData);
    }

    private void sendUnsubscriptionRequest(WebSocketClient client, Set<String> instruments) {
        JsonObject requestObject = constructSubscriptionRequest("unsub", instruments);
        byte[] binaryData = requestObject.toString().getBytes(StandardCharsets.UTF_8);
        logger.info("Sending Unsubscription Request: {}", requestObject);
        client.send(binaryData);
    }

    private JsonObject constructSubscriptionRequest(String method, Set<String> instruments) {
        JsonObject dataObject = new JsonObject();
        dataObject.addProperty("mode", "full");
        JsonArray instrumentKeysArray = new Gson().toJsonTree(instruments).getAsJsonArray();
        dataObject.add("instrumentKeys", instrumentKeysArray);

        JsonObject mainObject = new JsonObject();
        mainObject.addProperty("guid", UUID.randomUUID().toString());
        mainObject.addProperty("method", method);
        mainObject.add("data", dataObject);

        return mainObject;
    }

    private void publishMarketEvent(String token, MarketDataFeedV3.Feed feed) {
        long sequence = marketEventRingBuffer.next();
        try {
            MarketEvent event = marketEventRingBuffer.get(sequence);
            event.setSymbol(token);

            if (feed.hasLtpc()) {
                MarketDataFeedV3.LTPC ltpc = feed.getLtpc();
                event.setLtp(ltpc.getLtp());
                event.setLtt(ltpc.getLtt());
                event.setLtq(ltpc.getLtq());
                event.setCp(ltpc.getCp());
            }

            if (feed.hasFullFeed() && feed.getFullFeed().hasMarketFF()) {
                MarketDataFeedV3.MarketFullFeed mff = feed.getFullFeed().getMarketFF();
                event.setAtp(mff.getAtp());
                event.setVtt(mff.getVtt());
                event.setOi(mff.getOi());
                event.setIv(mff.getIv());
                event.setTbq((long)mff.getTbq());
                event.setTsq((long)mff.getTsq());
            }

            event.setTs(System.currentTimeMillis());
        } finally {
            marketEventRingBuffer.publish(sequence);
        }
    }

    private void publishRawFeedUpdate(String token, MarketDataFeedV3.Feed feed) {
        long sequence = rawFeedRingBuffer.next();
        try {
            RawFeedEvent event = rawFeedRingBuffer.get(sequence);
            event.setInstrumentKey(token);

            if (feed.hasLtpc()) {
                event.setLtp(feed.getLtpc().getLtp());
            }

            if (feed.hasFullFeed() && feed.getFullFeed().hasMarketFF() && feed.getFullFeed().getMarketFF().hasMarketLevel()) {
                var quotes = feed.getFullFeed().getMarketFF().getMarketLevel().getBidAskQuoteList();
                if (quotes != null) {
                    event.setBids(quotes.stream()
                        .map(q -> new RawFeedEvent.BookEntry(q.getBidP(), q.getBidQ(), 0))
                        .collect(Collectors.toList()));

                    event.setAsks(quotes.stream()
                        .map(q -> new RawFeedEvent.BookEntry(q.getAskP(), q.getAskQ(), 0))
                        .collect(Collectors.toList()));
                }
            }
        } finally {
            rawFeedRingBuffer.publish(sequence);
        }
    }
    public void setStrikeSubscriber(DynamicStrikeSubscriber subscriber) {
        this.strikeSubscriber = subscriber;
    }
    public void disconnect() {
        if (webSocketClient != null) {
            webSocketClient.close();
        }
    }
    public void subscribe(Set<String> newInstrumentKeys) {
        this.instrumentKeys.addAll(newInstrumentKeys);
        if (webSocketClient != null && webSocketClient.isOpen()) {
            sendSubscriptionRequest(webSocketClient, newInstrumentKeys);
        }
    }

    public void unsubscribe(Set<String> instrumentKeysToRemove) {
        this.instrumentKeys.removeAll(instrumentKeysToRemove);
        if (webSocketClient != null && webSocketClient.isOpen()) {
            sendUnsubscriptionRequest(webSocketClient, instrumentKeysToRemove);
        }
    }
}

// Helper class for JSON deserialization of the auth response
class WebSocketAuthResponse {
    private String status;
    private Data data;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Data getData() { return data; }
    public void setData(Data data) { this.data = data; }

    public static class Data {
        @SerializedName("authorized_redirect_uri")
        private String authorizedRedirectUri;

        public String getAuthorizedRedirectUri() { return authorizedRedirectUri; }
        public void setAuthorizedRedirectUri(String authorizedRedirectUri) { this.authorizedRedirectUri = authorizedRedirectUri; }
    }
}
