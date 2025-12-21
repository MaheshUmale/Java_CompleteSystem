package com.trading.hf.dashboard;

import com.google.gson.Gson;
import com.trading.hf.AuctionProfileCalculator;
import com.trading.hf.SignalEngine;
import com.trading.hf.VolumeBarGenerator;
import com.trading.hf.VolumeBar;
public class DashboardBridge {
    private static final Gson gson = new Gson();

    public static void start(
            VolumeBarGenerator volumeBarGenerator,
            SignalEngine signalEngine,
            AuctionProfileCalculator auctionProfileCalculator
    ) {
        DashboardService dashboardService = new DashboardService();
        dashboardService.start();

        volumeBarGenerator.setDashboardConsumer(volumeBar -> {
            // Get the latest state and profile for the symbol
            SignalEngine.AuctionState currentState = signalEngine.getAuctionState(volumeBar.getSymbol());
            AuctionProfileCalculator.MarketProfile profile = auctionProfileCalculator.getProfile(volumeBar.getSymbol());

            // Create the composite event
            DashboardEvent event = DashboardEvent.from(volumeBar, currentState, profile);

            // Serialize and broadcast
            String json = gson.toJson(event);
            dashboardService.broadcast(json);
        });

        Runtime.getRuntime().addShutdownHook(new Thread(dashboardService::stop));
    }
}
