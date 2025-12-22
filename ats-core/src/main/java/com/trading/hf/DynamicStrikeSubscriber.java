package com.trading.hf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class DynamicStrikeSubscriber {

    private static final Logger logger = LoggerFactory.getLogger(DynamicStrikeSubscriber.class);
    private final Consumer<Set<String>> subscriptionUpdateConsumer;
    private final InstrumentMaster instrumentMaster;
    private final String underlying;
    private int currentATM = 0;
    private LocalDate currentExpiry = null;
    private LocalDate lastExpiryCheckDate = null;

    public DynamicStrikeSubscriber(
            Consumer<Set<String>> subscriptionUpdateConsumer,
            InstrumentMaster instrumentMaster,
            String underlying
    ) {
        this.subscriptionUpdateConsumer = subscriptionUpdateConsumer;
        this.instrumentMaster = instrumentMaster;
        this.underlying = underlying;
    }

    public void onNiftySpotPrice(double spotPrice) {
        if (lastExpiryCheckDate == null || !LocalDate.now().isEqual(lastExpiryCheckDate)) {
            instrumentMaster.findNearestExpiry(underlying, LocalDate.now()).ifPresentOrElse(expiry -> {
                currentExpiry = expiry;
                lastExpiryCheckDate = LocalDate.now();
                logger.info("Set new expiry {} for underlying {}", currentExpiry, underlying);
            }, () -> {
                logger.error("Could not find nearest expiry for underlying: {}", underlying);
            });
        }

        int newATM = (int) (Math.round(spotPrice / 50.0) * 50);
        if (newATM != currentATM) {
            currentATM = newATM;
            logger.info("New ATM strike for {} is {}", underlying, currentATM);
            updateSubscriptions();
        }
    }

    private void updateSubscriptions() {
        if (currentExpiry == null) {
            logger.warn("Cannot update subscriptions, expiry is not set for {}", underlying);
            return;
        }

        Set<String> newSubscriptions = new HashSet<>();
        for (int i = -4; i <= 4; i++) {
            int strike = currentATM + (i * 50);
            instrumentMaster.findInstrumentKey(underlying, strike, "CE", currentExpiry).ifPresent(newSubscriptions::add);
            instrumentMaster.findInstrumentKey(underlying, strike, "PE", currentExpiry).ifPresent(newSubscriptions::add);
        }

        if (!newSubscriptions.isEmpty()) {
            logger.info("Subscribing to {} new instruments for underlying {}", newSubscriptions.size(), underlying);
            subscriptionUpdateConsumer.accept(newSubscriptions);
        } else {
            logger.warn("No instruments found for ATM {} and expiry {}", currentATM, currentExpiry);
        }
    }
}
