package com.trading.hf.dashboard;

import com.google.gson.Gson;
import com.trading.hf.VolumeBar;
import com.trading.hf.VolumeBarGenerator;

public class DashboardBridge {
    private static final Gson gson = new Gson();

    public static void start(VolumeBarGenerator volumeBarGenerator) {
        DashboardService dashboardService = new DashboardService();
        dashboardService.start();

        volumeBarGenerator.setDashboardConsumer(volumeBar -> {
            String json = gson.toJson(volumeBar);
            dashboardService.broadcast(json);
        });

        Runtime.getRuntime().addShutdownHook(new Thread(dashboardService::stop));
    }
}
