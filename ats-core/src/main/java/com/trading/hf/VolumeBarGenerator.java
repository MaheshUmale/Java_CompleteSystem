package com.trading.hf;

import com.lmax.disruptor.EventHandler;
import com.upstox.marketdatafeeder.rpc.proto.FeedResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class VolumeBarGenerator implements EventHandler<MarketEvent> {

    private final long volumeThreshold;
    private final Map<String, VolumeBar> volumeBars = new HashMap<>();
    private final Consumer<VolumeBar> onVolumeBar;
    private Consumer<VolumeBar> dashboardConsumer;

    public VolumeBarGenerator(long volumeThreshold, Consumer<VolumeBar> onVolumeBar) {
        this.volumeThreshold = volumeThreshold;
        this.onVolumeBar = onVolumeBar;
    }

    public void setDashboardConsumer(Consumer<VolumeBar> dashboardConsumer) {
        this.dashboardConsumer = dashboardConsumer;
    }

    @Override
    public void onEvent(MarketEvent event, long sequence, boolean endOfBatch) {
        FeedResponse feedResponse = event.getFeedResponse();
        feedResponse.getFeedsMap().forEach((key, feed) -> {
            VolumeBar bar = volumeBars.computeIfAbsent(key, k -> new VolumeBar(k, volumeThreshold));
            long signedVolume = 0;
            if (feed.getFullFeed().getMarketFF().getLtpc().getLtp() >= feed.getFullFeed().getMarketFF().getMarketLevel().getBidAskQuote(0).getAskP()) {
                signedVolume = feed.getFullFeed().getMarketFF().getLtpc().getLtq();
            } else if (feed.getFullFeed().getMarketFF().getLtpc().getLtp() <= feed.getFullFeed().getMarketFF().getMarketLevel().getBidAskQuote(0).getBidP()) {
                signedVolume = -feed.getFullFeed().getMarketFF().getLtpc().getLtq();
            }
            bar.addTick(
                    feed.getFullFeed().getMarketFF().getLtpc().getLtt(),
                    feed.getFullFeed().getMarketFF().getLtpc().getLtp(),
                    feed.getFullFeed().getMarketFF().getLtpc().getLtq(),
                    signedVolume
            );

            if (bar.isClosed()) {
                if (onVolumeBar != null) {
                    onVolumeBar.accept(bar);
                }
                if (dashboardConsumer != null) {
                    dashboardConsumer.accept(bar);
                }
                volumeBars.put(key, new VolumeBar(key, volumeThreshold));
            }
        });
    }
}
