package com.trading.hf;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args) {
        // --- Configuration ---
        String runMode = ConfigLoader.getProperty("run.mode", "simulation");
        boolean questDbEnabled = ConfigLoader.getBooleanProperty("questdb.enabled", false);
        long volumeThreshold = 500;
        String dataDirectory = "data";

        // --- Initialization ---
        boolean dashboardEnabled = ConfigLoader.getBooleanProperty("dashboard.enabled", true);
        QuestDBWriter questDBWriter = questDbEnabled ? new QuestDBWriter() : null;
        RawFeedWriter rawFeedWriter = new RawFeedWriter();

        AuctionProfileCalculator auctionProfileCalculator = new AuctionProfileCalculator();
        SignalEngine signalEngine = new SignalEngine(auctionProfileCalculator);
        IndexWeightCalculator indexWeightCalculator = new IndexWeightCalculator("IndexWeights.json");
        OptionChainProvider optionChainProvider = new OptionChainProvider();
        InstrumentMaster instrumentMaster = new InstrumentMaster("instrument-master.json");

        VolumeBarGenerator volumeBarGenerator = new VolumeBarGenerator(volumeThreshold, bar -> {
            auctionProfileCalculator.onVolumeBar(bar);
            signalEngine.onVolumeBar(bar);
            System.out.println(String.format("New Volume Bar: %s | O: %.2f H: %.2f L: %.2f C: %.2f V: %d",
                    bar.getSymbol(), bar.getOpen(), bar.getHigh(), bar.getLow(), bar.getClose(), bar.getVolume()));
        });

        DisruptorManager disruptorManager = new DisruptorManager(
                questDBWriter,
                rawFeedWriter,
                volumeBarGenerator,
                indexWeightCalculator,
                optionChainProvider
        );

        if (dashboardEnabled) {
            com.trading.hf.dashboard.DashboardBridge.start(
                volumeBarGenerator,
                signalEngine,
                auctionProfileCalculator,
                indexWeightCalculator,
                optionChainProvider
            );
        }

        if ("live".equalsIgnoreCase(runMode)) {
            // --- Live Mode ---
            System.out.println("Starting application in LIVE mode.");
            String accessToken = ConfigLoader.getProperty("upstox.accessToken");
            if (accessToken == null || "YOUR_ACCESS_TOKEN_HERE".equals(accessToken)) {
                System.err.println("FATAL: upstox.accessToken is not configured in config.properties.");
                return;
            }

            Set<String> initialInstrumentKeys = new HashSet<>();
            initialInstrumentKeys.add("NSE_INDEX|Nifty 50");
            initialInstrumentKeys.add("NSE_INDEX|Nifty Bank");
            initialInstrumentKeys.add("NSE_FO|FUTIDX NIFTY 30MAY24"); // Near-month Nifty Future
            initialInstrumentKeys.addAll(indexWeightCalculator.getInstrumentKeys());

            UpstoxMarketDataStreamer marketDataStreamer = new UpstoxMarketDataStreamer(
                    accessToken,
                    disruptorManager.getMarketEventRingBuffer(),
                    disruptorManager.getRawFeedRingBuffer(),
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
            }, instrumentMaster);

            marketDataStreamer.setStrikeSubscriber(strikeSubscriber);
            marketDataStreamer.connect();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                marketDataStreamer.disconnect();
                disruptorManager.shutdown();
                if (questDBWriter != null) questDBWriter.close();
                rawFeedWriter.close();
            }));

        } else {
            // --- Simulation Mode ---
            System.out.println("Starting application in SIMULATION mode.");

            String replaySource = ConfigLoader.getProperty("replay.source", "sample_data");
            IDataReplayer replayer;

            switch (replaySource) {
                case "sample_data":
                    replayer = new MySampleDataReplayer(disruptorManager.getMarketEventRingBuffer(), dataDirectory);
                    break;
                // Add cases for other replay sources here in the future
                default:
                    System.err.println("FATAL: Unknown replay.source configured: " + replaySource);
                    return;
            }

            replayer.start(); // This will block until replay is complete

            try {
                // Give logs a moment to flush before shutting down
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            System.out.println("Simulation finished. Server will remain active for dashboard connection.");

            // Keep the main thread alive to allow the dashboard to be viewed.
            // The shutdown hook will handle closing resources.
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutdown hook initiated.");
                disruptorManager.shutdown();
                if (questDBWriter != null) questDBWriter.close();
                System.out.println("Resources released.");
            }));

            while (true) {
                try {
                    Thread.sleep(10000); // Sleep indefinitely
                } catch (InterruptedException e) {
                    System.out.println("Main thread interrupted, shutting down.");
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }
}
