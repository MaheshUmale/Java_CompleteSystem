package com.trading.hf;

import com.lmax.disruptor.EventHandler;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class VolumeBarGenerator implements EventHandler<MarketEvent> {

    private final long volumeThreshold;
    private final ConcurrentHashMap<String, VolumeBar> runningBars = new ConcurrentHashMap<>();
    private final Consumer<VolumeBar> barConsumer;

    public VolumeBarGenerator(long volumeThreshold, Consumer<VolumeBar> barConsumer) {
        this.volumeThreshold = volumeThreshold;
        this.barConsumer = barConsumer;
    }

    @Override
    public void onEvent(MarketEvent event, long sequence, boolean endOfBatch) {
        runningBars.compute(event.getSymbol(), (symbol, bar) -> {
            if (bar == null) {
                bar = new VolumeBar(symbol, event.getLtt(), event.getLtp(), event.getLtq());
            } else {
                int side = determineSide(event);
                bar.addTick(event.getLtp(), event.getLtq(), side);
            }

            if (bar.getVolume() >= volumeThreshold) {
                bar.setOrderBookImbalance(calculateOBI(event));
                barConsumer.accept(bar);
                return null; // Start a new bar
            }
            return bar;
        });
    }

    private int determineSide(MarketEvent event) {
        // TODO: This is a placeholder logic. A more sophisticated approach would be to
        // use the bid/ask prices from the market depth data to determine the aggressor side.
        if (event.getLtp() > event.getCp()) {
            return 1; // Buy
        } else if (event.getLtp() < event.getCp()) {
            return -1; // Sell
        }
        return 0; // Neutral
    }

    private double calculateOBI(MarketEvent event) {
        if (event.getTbq() + event.getTsq() == 0) {
            return 0;
        }
        return (event.getTbq() - event.getTsq()) / (event.getTbq() + event.getTsq());
    }
}
