package com.trading.hf.dashboard;

import com.google.gson.Gson;
import com.trading.hf.*;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class DashboardBridge {
    private static final Gson gson = new Gson();
    private static DashboardService dashboardService = null;
    private static final Object lock = new Object();

    public static void start(
            VolumeBarGenerator volumeBarGenerator,
            SignalEngine signalEngine,
            AuctionProfileCalculator auctionProfileCalculator,
            IndexWeightCalculator indexWeightCalculator,
            OptionChainProvider optionChainProvider
    ) {
        synchronized (lock) {
            if (dashboardService == null) {
                dashboardService = new DashboardService();
                dashboardService.start();
                Runtime.getRuntime().addShutdownHook(new Thread(dashboardService::stop));
            }
        }

        volumeBarGenerator.setDashboardConsumer(volumeBar -> {
            DashboardViewModel viewModel = new DashboardViewModel();

            // 1. Populate Header Info
            viewModel.timestamp = System.currentTimeMillis();
            viewModel.symbol = volumeBar.getSymbol();
            viewModel.spot = volumeBar.getClose();

            // 2. Populate Auction Profile
            AuctionProfileCalculator.MarketProfile profile = auctionProfileCalculator.getProfile(volumeBar.getSymbol());
            if (profile != null) {
                viewModel.auctionProfile = new DashboardViewModel.MarketProfileViewModel();
                viewModel.auctionProfile.vah = profile.getVah();
                viewModel.auctionProfile.val = profile.getVal();
                viewModel.auctionProfile.poc = profile.getPoc();
            }

            // 3. Populate Heavyweights
            viewModel.heavyweights = indexWeightCalculator.getHeavyweights().values().stream()
                    .map(hw -> {
                        DashboardViewModel.HeavyweightViewModel hwvm = new DashboardViewModel.HeavyweightViewModel();
                        hwvm.name = hw.getName();
                        hwvm.weight = String.format("%.2f%%", hw.getWeight() * 100);
                        hwvm.delta = hw.getDelta();
                        return hwvm;
                    })
                    .collect(Collectors.toList());
            viewModel.aggregateWeightedDelta = indexWeightCalculator.getAggregateWeightedDelta();

            // 4. Populate Option Chain by transforming the DTO
            viewModel.optionChain = optionChainProvider.getOptionChainWindow().stream()
                    .map(dto -> {
                        DashboardViewModel.OptionViewModel ovm = new DashboardViewModel.OptionViewModel();
                        ovm.strike = dto.getStrike();
                        ovm.type = dto.getType();
                        ovm.ltp = dto.getLtp();
                        ovm.oiChangePercent = dto.getOiChangePercent();
                        ovm.sentiment = dto.getSentiment();
                        return ovm;
                    })
                    .collect(Collectors.toList());

            // 5. Populate Sentiment & Alerts
            SignalEngine.AuctionState auctionState = signalEngine.getAuctionState(volumeBar.getSymbol());
            viewModel.auctionState = (auctionState != null) ? auctionState.toString() : "ROTATION";
            viewModel.alerts = new ArrayList<>(); // Placeholder

            // 6. Populate Trade Panel
            viewModel.thetaGuard = 1200; // Placeholder

            // Serialize and broadcast
            String json = gson.toJson(viewModel);
            if (dashboardService != null) {
                dashboardService.broadcast(json);
            }
        });
    }
}
