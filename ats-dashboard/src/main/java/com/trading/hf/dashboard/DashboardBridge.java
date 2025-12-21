package com.trading.hf.dashboard;

import com.google.gson.Gson;
import com.trading.hf.AuctionProfileCalculator;
import com.trading.hf.VolumeBar;
import com.trading.hf.VolumeBarGenerator;

public class DashboardBridge {
    private static final Gson gson = new Gson();

    public static void start(VolumeBarGenerator volumeBarGenerator, AuctionProfileCalculator auctionProfileCalculator) {
        DashboardService dashboardService = new DashboardService();
        dashboardService.start();

        volumeBarGenerator.setDashboardConsumer(volumeBar -> {
            AuctionProfileCalculator.MarketProfile profile = auctionProfileCalculator.getProfile(volumeBar.getSymbol());
            if (profile != null) {
                DashboardEvent event = new DashboardEvent(volumeBar, profile);
                String json = gson.toJson(event);
                dashboardService.broadcast(json);
            }
        });

        Runtime.getRuntime().addShutdownHook(new Thread(dashboardService::stop));
    }
}
