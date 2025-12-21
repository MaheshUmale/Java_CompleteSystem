package com.trading.hf;

import com.upstox.marketdatafeeder.rpc.proto.FeedResponse;

public class MarketEvent {
    private FeedResponse feedResponse;

    public FeedResponse getFeedResponse() {
        return feedResponse;
    }

    public void setFeedResponse(FeedResponse feedResponse) {
        this.feedResponse = feedResponse;
    }
}
