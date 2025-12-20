package com.trading.hf;

import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.EventHandlerGroup;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.YieldingWaitStrategy;
import java.util.concurrent.ThreadFactory;
import java.util.ArrayList;
import java.util.List;
import com.lmax.disruptor.EventHandler;


public class DisruptorManager {

    private final Disruptor<MarketEvent> disruptor;
    private final RingBuffer<MarketEvent> ringBuffer;

    @SuppressWarnings("unchecked")
    public DisruptorManager(QuestDBWriter questDBWriter, VolumeBarGenerator volumeBarGenerator, IndexWeightCalculator indexWeightCalculator) {
        ThreadFactory threadFactory = Thread.ofVirtual().factory();
        disruptor = new Disruptor<>(
                MarketEvent.EVENT_FACTORY,
                65536,
                threadFactory,
                ProducerType.SINGLE,
                new YieldingWaitStrategy()
        );

        List<EventHandler<MarketEvent>> handlers = new ArrayList<>();
        handlers.add(volumeBarGenerator);
        handlers.add(indexWeightCalculator);
        if (questDBWriter != null) {
            handlers.add(questDBWriter);
        }

        disruptor.handleEventsWith(handlers.toArray(new EventHandler[0]));

        ringBuffer = disruptor.start();
    }

    public RingBuffer<MarketEvent> getRingBuffer() {
        return ringBuffer;
    }

    public void shutdown() {
        disruptor.shutdown();
    }
}
