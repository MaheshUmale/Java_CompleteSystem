package com.trading.hf;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class DynamicStrikeSubscriber {

    private final Consumer<Set<String>> subscriptionUpdateConsumer;
    private int currentATM = 0;

    public DynamicStrikeSubscriber(Consumer<Set<String>> subscriptionUpdateConsumer) {
        this.subscriptionUpdateConsumer = subscriptionUpdateConsumer;
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
            newSubscriptions.add(generateInstrumentKey("NFO", "NIFTY", currentATM + (i * 50), "CE"));
            newSubscriptions.add(generateInstrumentKey("NFO", "NIFTY", currentATM + (i * 50), "PE"));
        }
        subscriptionUpdateConsumer.accept(newSubscriptions);
    }

    private String generateInstrumentKey(String segment, String underlying, int strike, String optionType) {
        // This is a simplified key generation logic. The actual key format will depend
        // on the specific requirements of the Upstox API.
        return String.format("%s:%s:%d:%s", segment, underlying, strike, optionType);
    }
}
