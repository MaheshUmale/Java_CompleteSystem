package com.trading.hf;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args) {
        // --- Configuration ---
        String accessToken = ConfigLoader.getProperty("upstox.accessToken");
        long volumeThreshold = 5000;
        Set<String> initialInstrumentKeys = new HashSet<>();
        initialInstrumentKeys.add("NSE_INDEX|Nifty 50");
        initialInstrumentKeys.add("NSE_INDEX|Nifty Bank");

        // --- Initialization ---
        QuestDBWriter questDBWriter = new QuestDBWriter();
        VolumeBarGenerator volumeBarGenerator = new VolumeBarGenerator(volumeThreshold, bar -> {
            System.out.println("New Volume Bar: " + bar.getSymbol() + " - " + bar.getClose());
        });
        IndexWeightCalculator indexWeightCalculator = new IndexWeightCalculator("IndexWeights.json");

        DisruptorManager disruptorManager = new DisruptorManager(
                questDBWriter,
                volumeBarGenerator,
                indexWeightCalculator
        );

        UpstoxMarketDataStreamer marketDataStreamer = new UpstoxMarketDataStreamer(
                accessToken,
                disruptorManager.getRingBuffer(),
                initialInstrumentKeys
        );

        DynamicStrikeSubscriber strikeSubscriber = new DynamicStrikeSubscriber(newSubscriptions -> {
            Set<String> currentSubscriptions = new HashSet<>(initialInstrumentKeys);
            Set<String> toSubscribe = newSubscriptions.stream()
                .filter(s -> !currentSubscriptions.contains(s))
                .collect(Collectors.toSet());
            Set<String> toUnsubscribe = currentSubscriptions.stream()
                .filter(s -> !newSubscriptions.contains(s) && !initialInstrumentKeys.contains(s))
                .collect(Collectors.toSet());

            if (!toSubscribe.isEmpty()) {
                marketDataStreamer.subscribe(toSubscribe);
                System.out.println("Subscribing to: " + toSubscribe);
            }
            if (!toUnsubscribe.isEmpty()) {
                marketDataStreamer.unsubscribe(toUnsubscribe);
                System.out.println("Unsubscribing from: " + toUnsubscribe);
            }
        });

        marketDataStreamer.setStrikeSubscriber(strikeSubscriber);


        // --- Start the System ---
        marketDataStreamer.connect();

        // --- Shutdown Hook ---
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            marketDataStreamer.disconnect();
            disruptorManager.shutdown();
            questDBWriter.close();
        }));
    }
}
