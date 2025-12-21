package com.trading.hf;

import com.lmax.disruptor.EventHandler;
import com.upstox.marketdatafeeder.rpc.proto.FeedResponse;

import java.util.HashMap;
import java.util.Map;

public class ThetaExitGuard implements EventHandler<MarketEvent> {

    private final Map<String, Double> lastLtp = new HashMap<>();
    private final Map<String, Long> lastTimestamp = new HashMap<>();

    @Override
    public void onEvent(MarketEvent event, long sequence, boolean endOfBatch) {
        FeedResponse feedResponse = event.getFeedResponse();
        feedResponse.getFeedsMap().forEach((key, feed) -> {
            double ltp = feed.getFullFeed().getMarketFF().getLtpc().getLtp();
            long timestamp = feed.getFullFeed().getMarketFF().getLtpc().getLtt();
            double theta = feed.getFullFeed().getMarketFF().getOptionGreeks().getTheta();

            if (lastLtp.containsKey(key)) {
                // This is where you would implement the theta exit logic
            }

            lastLtp.put(key, ltp);
            lastTimestamp.put(key, timestamp);
        });
    }
}
