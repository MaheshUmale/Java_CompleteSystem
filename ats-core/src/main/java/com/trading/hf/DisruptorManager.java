package com.trading.hf;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.util.DaemonThreadFactory;

public class DisruptorManager {

    private final Disruptor<MarketEvent> disruptor;
    private final RingBuffer<MarketEvent> ringBuffer;

    @SuppressWarnings("unchecked")
    public DisruptorManager(
            EventHandler<MarketEvent>... handlers) {
        int bufferSize = 1024;
        disruptor = new Disruptor<>(
                MarketEvent::new,
                bufferSize,
                DaemonThreadFactory.INSTANCE);

        disruptor.handleEventsWith(handlers);

        this.ringBuffer = disruptor.getRingBuffer();
        disruptor.start();
    }

    public RingBuffer<MarketEvent> getRingBuffer() {
        return ringBuffer;
    }

    public void shutdown() {
        disruptor.shutdown();
    }
}
