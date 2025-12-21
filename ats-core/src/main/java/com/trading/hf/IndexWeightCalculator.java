package com.trading.hf;

import com.google.gson.Gson;
import com.lmax.disruptor.EventHandler;
import com.upstox.marketdatafeeder.rpc.proto.FeedResponse;

import java.io.InputStreamReader;
import java.util.Map;

public class IndexWeightCalculator implements EventHandler<MarketEvent> {

    private final Map<String, Double> indexWeights;

    public IndexWeightCalculator(String filename) {
        try {
            indexWeights = new Gson().fromJson(new InputStreamReader(ConfigLoader.class.getClassLoader().getResourceAsStream(filename)), Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Error loading index weights", e);
        }
    }

    @Override
    public void onEvent(MarketEvent event, long sequence, boolean endOfBatch) {
        FeedResponse feedResponse = event.getFeedResponse();
        feedResponse.getFeedsMap().forEach((key, feed) -> {
            if (indexWeights.containsKey(key)) {
                double weight = indexWeights.get(key);
                // This is where you would implement the index weight calculation
            }
        });
    }
}
