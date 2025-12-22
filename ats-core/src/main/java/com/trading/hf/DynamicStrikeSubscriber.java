package com.trading.hf;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class DynamicStrikeSubscriber {

    private final Consumer<Set<String>> subscriptionUpdateConsumer;
    private final InstrumentMaster instrumentMaster;
    private int currentATM = 0;

    public DynamicStrikeSubscriber(
            Consumer<Set<String>> subscriptionUpdateConsumer,
            InstrumentMaster instrumentMaster
    ) {
        this.subscriptionUpdateConsumer = subscriptionUpdateConsumer;
        this.instrumentMaster = instrumentMaster;
    }

    public void onNiftySpotPrice(double spotPrice) {
        int newATM = (int) (Math.round(spotPrice / 50.0) * 50);
        if (newATM != currentATM) {
            currentATM = newATM;
            updateSubscriptions();
        }
    }

    private void updateSubscriptions() {
        Set<String> newSubscriptions = new HashSet<>();
        for (int i = -2; i <= 2; i++) {
            int strike = currentATM + (i * 50);
            instrumentMaster.findInstrumentKey(strike, "CE").ifPresent(newSubscriptions::add);
            instrumentMaster.findInstrumentKey(strike, "PE").ifPresent(newSubscriptions::add);
        }

        if (!newSubscriptions.isEmpty()) {
            subscriptionUpdateConsumer.accept(newSubscriptions);
        }
    }
}
