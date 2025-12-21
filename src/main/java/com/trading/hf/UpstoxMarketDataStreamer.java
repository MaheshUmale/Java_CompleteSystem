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
import com.upstox.feeder.MarketUpdateV3.Feed;
import java.util.Set;

public class UpstoxMarketDataStreamer {

    private final RingBuffer<MarketEvent> ringBuffer;
    private final MarketDataStreamerV3 marketDataStreamer;
    private DynamicStrikeSubscriber strikeSubscriber;

    public UpstoxMarketDataStreamer(String accessToken, RingBuffer<MarketEvent> ringBuffer, Set<String> instrumentKeys) {
        this.ringBuffer = ringBuffer;

        ApiClient defaultClient = Configuration.getDefaultApiClient();
        OAuth oAuth = (OAuth) defaultClient.getAuthentication("OAUTH2");
        oAuth.setAccessToken(accessToken);

        this.marketDataStreamer = new MarketDataStreamerV3(defaultClient, instrumentKeys, Mode.FULL);

        marketDataStreamer.setOnMarketUpdateListener(new OnMarketUpdateV3Listener() {
            @Override
            public void onUpdate(MarketUpdateV3 marketUpdate) {
                marketUpdate.getFeeds().forEach((instrumentKey, feed) -> {
                    if (strikeSubscriber != null && instrumentKey.equals("NSE_INDEX|Nifty 50")) {
                        strikeSubscriber.onNiftySpotPrice(feed.getFullFeed().getMarketFF().getLtpc().getLtp());
                    }
                    publishMarketUpdate(instrumentKey, feed);
                });
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

    private void publishMarketUpdate(String instrumentKey, Feed feed) {
        long sequence = ringBuffer.next();
        try {
            MarketEvent event = ringBuffer.get(sequence);
            var marketFF = feed.getFullFeed().getMarketFF();
            var ltpc = marketFF.getLtpc();
            var optionGreeks = feed.getFullFeed().getMarketFF().getOptionGreeks();


            event.setSymbol(instrumentKey);
            event.setLtp(ltpc.getLtp());
            event.setLtt(ltpc.getLtt());
            event.setLtq(ltpc.getLtq());
            event.setCp(ltpc.getCp());
            event.setTbq(marketFF.getMarketLevel().getBidAskQuote().get(0).getBidQ());
            event.setTsq(marketFF.getMarketLevel().getBidAskQuote().get(0).getAskQ());
            event.setVtt(marketFF.getVtt());
            event.setOi(marketFF.getOi());
            event.setIv(marketFF.getIv());
            event.setAtp(marketFF.getAtp());
            event.setTheta(optionGreeks.getTheta());
            event.setTs(System.currentTimeMillis());
        } finally {
            ringBuffer.publish(sequence);
        }
    }
}
