package com.trading.hf;

import com.lmax.disruptor.RingBuffer;
import com.upstox.ApiClient;
import com.upstox.Configuration;
import com.upstox.auth.OAuth;
import com.upstox.feeder.MarketDataStreamerV3;
import com.upstox.feeder.MarketUpdateV3;
import com.upstox.feeder.listener.OnMarketUpdateV3Listener;
import com.upstox.feeder.listener.OnErrorListener;
import com.upstox.feeder.constants.Mode;

import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Collectors;

public class UpstoxMarketDataStreamer {

    private final RingBuffer<MarketEvent> marketEventRingBuffer;
    private final RingBuffer<RawFeedEvent> rawFeedRingBuffer;
    private final MarketDataStreamerV3 marketDataStreamer;
    private DynamicStrikeSubscriber strikeSubscriber;
    private final boolean persistenceEnabled;

    public UpstoxMarketDataStreamer(
            String accessToken,
            RingBuffer<MarketEvent> marketEventRingBuffer,
            RingBuffer<RawFeedEvent> rawFeedRingBuffer,
            Set<String> instrumentKeys
    ) {
        this.marketEventRingBuffer = marketEventRingBuffer;
        this.rawFeedRingBuffer = rawFeedRingBuffer;
        this.persistenceEnabled = ConfigLoader.getBooleanProperty("database.persistence.enabled", false);

        ApiClient defaultClient = Configuration.getDefaultApiClient();
        OAuth oAuth = (OAuth) defaultClient.getAuthentication("OAUTH2");
        oAuth.setAccessToken(accessToken);

        this.marketDataStreamer = new MarketDataStreamerV3(defaultClient, instrumentKeys, Mode.FULL);

        marketDataStreamer.setOnMarketUpdateListener(new OnMarketUpdateV3Listener() {
            @Override
            public void onUpdate(MarketUpdateV3 marketUpdate) {
                 if (strikeSubscriber != null && marketUpdate.getInstrumentToken().equals("NSE_INDEX|Nifty 50")) {
                     strikeSubscriber.onNiftySpotPrice(marketUpdate.getLtp());
                 }
                 publishMarketUpdate(marketUpdate);

                 if (persistenceEnabled) {
                     publishRawFeedUpdate(marketUpdate);
                 }
            }
        });

        marketDataStreamer.setOnErrorListener(new OnErrorListener() {
            @Override
            public void onError(Throwable t) {
                System.err.println("Error in MarketDataStreamer: " + t.getMessage());
            }
        });
    }

    public void setStrikeSubscriber(DynamicStrikeSubscriber strikeSubscriber) {
        this.strikeSubscriber = strikeSubscriber;
    }

    public void connect() {
        marketDataStreamer.connect();
    }

    public void disconnect() {
        marketDataStreamer.disconnect();
    }

    public void subscribe(Set<String> instrumentKeys) {
        marketDataStreamer.subscribe(instrumentKeys, Mode.FULL);
    }

    public void unsubscribe(Set<String> instrumentKeys) {
        marketDataStreamer.unsubscribe(instrumentKeys);
    }

    private void publishMarketUpdate(MarketUpdateV3 marketUpdate) {
        long sequence = marketEventRingBuffer.next();
         try {
             MarketEvent event = marketEventRingBuffer.get(sequence);
             event.setSymbol(marketUpdate.getInstrumentToken());
             event.setLtp(marketUpdate.getLtp());
             event.setLtt(marketUpdate.getLtt());
             event.setLtq(marketUpdate.getLtq());
             event.setCp(marketUpdate.getClosePrice());
             // Assuming tbq and tsq are total buy/sell quantities, which might be tbv/tsv
             event.setTbq(marketUpdate.getTbv());
             event.setTsq(marketUpdate.getTsv());
             event.setVtt(marketUpdate.getVtt());
             event.setOi(marketUpdate.getOi());
             event.setIv(0); // IV not available in MarketUpdateV3
             event.setAtp(marketUpdate.getAtp());
             event.setTs(System.currentTimeMillis());
         } finally {
             marketEventRingBuffer.publish(sequence);
         }
    }

    private void publishRawFeedUpdate(MarketUpdateV3 marketUpdate) {
        long sequence = rawFeedRingBuffer.next();
        try {
            RawFeedEvent event = rawFeedRingBuffer.get(sequence);
            event.setInstrumentKey(marketUpdate.getInstrumentToken());
            event.setTimestamp(marketUpdate.getLtt() > 0 ? marketUpdate.getLtt() : System.currentTimeMillis());
            event.setLtp(marketUpdate.getLtp());
            event.setLtq(marketUpdate.getLtq());

            event.setBids(
                marketUpdate.getBids().stream()
                    .map(b -> new RawFeedEvent.BookEntry(b.getPrice(), b.getQuantity(), b.getOrders()))
                    .collect(Collectors.toList())
            );

            event.setAsks(
                marketUpdate.getAsks().stream()
                    .map(a -> new RawFeedEvent.BookEntry(a.getPrice(), a.getQuantity(), a.getOrders()))
                    .collect(Collectors.toList())
            );

        } finally {
            rawFeedRingBuffer.publish(sequence);
        }
    }
}
