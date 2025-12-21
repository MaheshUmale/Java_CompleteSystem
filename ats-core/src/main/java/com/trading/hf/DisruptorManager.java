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

    private final Disruptor<MarketEvent> marketEventDisruptor;
    private final RingBuffer<MarketEvent> marketEventRingBuffer;

    private final Disruptor<RawFeedEvent> rawFeedDisruptor;
    private final RingBuffer<RawFeedEvent> rawFeedRingBuffer;

    @SuppressWarnings("unchecked")
    public DisruptorManager(
            QuestDBWriter questDBWriter,
            RawFeedWriter rawFeedWriter,
            VolumeBarGenerator volumeBarGenerator,
            IndexWeightCalculator indexWeightCalculator
    ) {
        ThreadFactory threadFactory = Thread.ofVirtual().factory();

        // Disruptor for processed MarketEvents
        marketEventDisruptor = new Disruptor<>(
                MarketEvent.EVENT_FACTORY,
                65536,
                threadFactory,
                ProducerType.SINGLE,
                new YieldingWaitStrategy()
        );

        List<EventHandler<MarketEvent>> marketEventHandlers = new ArrayList<>();
        marketEventHandlers.add(volumeBarGenerator);
        marketEventHandlers.add(indexWeightCalculator);
        if (questDBWriter != null) {
            marketEventHandlers.add(questDBWriter);
        }
        marketEventDisruptor.handleEventsWith(marketEventHandlers.toArray(new EventHandler[0]));
        marketEventRingBuffer = marketEventDisruptor.start();

        // Disruptor for RawFeedEvents
        rawFeedDisruptor = new Disruptor<>(
                RawFeedEvent::new, // Assuming RawFeedEvent has a default constructor
                65536,
                threadFactory,
                ProducerType.SINGLE,
                new YieldingWaitStrategy()
        );

        if (rawFeedWriter != null) {
            rawFeedDisruptor.handleEventsWith(rawFeedWriter);
        }
        rawFeedRingBuffer = rawFeedDisruptor.start();
    }

    public RingBuffer<MarketEvent> getMarketEventRingBuffer() {
        return marketEventRingBuffer;
    }

    public RingBuffer<RawFeedEvent> getRawFeedRingBuffer() {
        return rawFeedRingBuffer;
    }

    public void shutdown() {
        marketEventDisruptor.shutdown();
        rawFeedDisruptor.shutdown();
    }
}
