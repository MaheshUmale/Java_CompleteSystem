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
                if (marketUpdate.getFeeds() == null) return;

                // Loop through the map: Key is instrumentToken, Value is Feed
                marketUpdate.getFeeds().forEach((instrumentToken, feed) -> {

                    // Handle Nifty Spot
                    if (strikeSubscriber != null && "NSE_INDEX|Nifty 50".equals(instrumentToken)) {
                        if (feed.getLtpc() != null) {
                            strikeSubscriber.onNiftySpotPrice(feed.getLtpc().getLtp());
                        }
                    }

                    publishMarketEvent(instrumentToken, feed);

                    if (persistenceEnabled) {
                        publishRawFeedUpdate(instrumentToken, feed);
                    }
                });
            }
        });

        marketDataStreamer.setOnErrorListener(new OnErrorListener() {
            @Override
            public void onError(Throwable t) {
                System.err.println("Upstox Streamer Error: " + t.getMessage());
            }
        });
    }

    private void publishMarketEvent(String token, MarketUpdateV3.Feed feed) {
        long sequence = marketEventRingBuffer.next();
        try {
            MarketEvent event = marketEventRingBuffer.get(sequence);
            event.setSymbol(token);

            // LTPC block
            if (feed.getLtpc() != null) {
                event.setLtp(feed.getLtpc().getLtp());
                event.setLtt(feed.getLtpc().getLtt());
                event.setLtq(feed.getLtpc().getLtq());
                event.setCp(feed.getLtpc().getCp());
            }

            // FullFeed -> MarketFullFeed block
            if (feed.getFullFeed() != null && feed.getFullFeed().getMarketFF() != null) {
                MarketUpdateV3.MarketFullFeed mff = feed.getFullFeed().getMarketFF();
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

    private void publishRawFeedUpdate(String token, MarketUpdateV3.Feed feed) {
        long sequence = rawFeedRingBuffer.next();
        try {
            RawFeedEvent event = rawFeedRingBuffer.get(sequence);
            event.setInstrumentKey(token);

            if (feed.getLtpc() != null) {
                event.setLtp(feed.getLtpc().getLtp());
            }

            // Handle Depth (Bids/Asks)
            if (feed.getFullFeed() != null &&
                feed.getFullFeed().getMarketFF() != null &&
                feed.getFullFeed().getMarketFF().getMarketLevel() != null) {

                var quotes = feed.getFullFeed().getMarketFF().getMarketLevel().getBidAskQuote();

                if (quotes != null) {
                    event.setBids(quotes.stream()
                        .map(q -> new RawFeedEvent.BookEntry(q.getBidP(), (int)q.getBidQ(), 0))
                        .collect(Collectors.toList()));

                    event.setAsks(quotes.stream()
                        .map(q -> new RawFeedEvent.BookEntry(q.getAskP(), (int)q.getAskQ(), 0))
                        .collect(Collectors.toList()));
                }
            }
        } finally {
            rawFeedRingBuffer.publish(sequence);
        }
    }

    public void setStrikeSubscriber(DynamicStrikeSubscriber subscriber) { this.strikeSubscriber = subscriber; }
    public void connect() { marketDataStreamer.connect(); }
    public void disconnect() { marketDataStreamer.disconnect(); }
    public void subscribe(Set<String> instrumentKeys) { marketDataStreamer.subscribe(instrumentKeys, Mode.FULL); }
    public void unsubscribe(Set<String> instrumentKeys) { marketDataStreamer.unsubscribe(instrumentKeys); }
}
