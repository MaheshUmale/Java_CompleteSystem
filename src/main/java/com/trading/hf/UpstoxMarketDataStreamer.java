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
                // if (strikeSubscriber != null && marketUpdate.getInstrumentToken().equals("NSE_INDEX|Nifty 50")) {
                //     strikeSubscriber.onNiftySpotPrice(marketUpdate.getLtp());
                // }
                // publishMarketUpdate(marketUpdate);
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
        long sequence = ringBuffer.next();
        // try {
        //     MarketEvent event = ringBuffer.get(sequence);
        //     event.setSymbol(marketUpdate.getInstrumentToken());
        //     event.setLtp(marketUpdate.getLtp());
        //     event.setLtt(marketUpdate.getLtt());
        //     event.setLtq(marketUpdate.getLtq());
        //     event.setCp(marketUpdate.getClosePrice());
        //     event.setTbq(marketUpdate.getTbv());
        //     event.setTsq(marketUpdate.getTsv());
        //     event.setVtt(marketUpdate.getVtt());
        //     event.setOi(marketUpdate.getOi());
        //     event.setIv(0); // IV not available in MarketUpdateV3
        //     event.setAtp(marketUpdate.getAtp());
        //     event.setTs(System.currentTimeMillis());
        // } finally {
        //     ringBuffer.publish(sequence);
        // }
    }
}
