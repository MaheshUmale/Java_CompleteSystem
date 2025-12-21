package com.trading.hf.dashboard;

import com.trading.hf.AuctionProfileCalculator;
import com.trading.hf.VolumeBar;

public class DashboardEvent {
    private final VolumeBar volumeBar;
    private final AuctionProfileCalculator.MarketProfile marketProfile;

    public DashboardEvent(VolumeBar volumeBar, AuctionProfileCalculator.MarketProfile marketProfile) {
        this.volumeBar = volumeBar;
        this.marketProfile = marketProfile;
    }
}
