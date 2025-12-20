package com.trading.hf;

import com.lmax.disruptor.EventHandler;

public class ThetaExitGuard implements EventHandler<MarketEvent> {

    @Override
    public void onEvent(MarketEvent event, long sequence, boolean endOfBatch) {
        // TODO: Implement theta decay monitoring logic
    }
}
