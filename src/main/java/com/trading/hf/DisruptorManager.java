package com.trading.hf;

import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.YieldingWaitStrategy;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class DisruptorManager {

    private final Disruptor<MarketEvent> disruptor;
    private final RingBuffer<MarketEvent> ringBuffer;

    @SuppressWarnings("unchecked")
    public DisruptorManager(QuestDBWriter questDBWriter, VolumeBarGenerator volumeBarGenerator, IndexWeightCalculator indexWeightCalculator, ThetaExitGuard thetaExitGuard) {
        ThreadFactory threadFactory = Thread.ofVirtual().factory();
        disruptor = new Disruptor<>(
                MarketEvent.EVENT_FACTORY,
                65536,
                threadFactory,
                ProducerType.SINGLE,
                new YieldingWaitStrategy()
        );

        disruptor.handleEventsWith(questDBWriter, volumeBarGenerator, indexWeightCalculator, thetaExitGuard);

        ringBuffer = disruptor.start();
    }

    public RingBuffer<MarketEvent> getRingBuffer() {
        return ringBuffer;
    }

    public void shutdown() {
        disruptor.shutdown();
    }
}
