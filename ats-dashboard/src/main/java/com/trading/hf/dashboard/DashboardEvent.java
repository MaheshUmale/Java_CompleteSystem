package com.trading.hf.dashboard;

import com.trading.hf.AuctionProfileCalculator;
import com.trading.hf.SignalEngine;
import com.trading.hf.VolumeBar;

import java.util.Collections;
import java.util.List;

// This class is a DTO (Data Transfer Object) designed to be serialized to JSON
// for the frontend dashboard.
public class DashboardEvent {
    public long timestamp;
    public double spot;
    public double future; // Placeholder
    public double weighted_delta; // Placeholder
    public String auction_state;
    public double pcr; // Placeholder
    public int theta_guard_sec; // Placeholder
    public List<Heavyweight> heavyweights;
    public List<Option> option_window;
    public MarketProfileData market_profile;

    // Inner classes to structure the JSON correctly
    public static class Heavyweight {
        public String name;
        public double delta;
        public String weight;

        public Heavyweight(String name, double delta, String weight) {
            this.name = name;
            this.delta = delta;
            this.weight = weight;
        }
    }

    public static class Option {
        public int strike;
        public String type;
        public double ltp;
        public double oi_chg;

        public Option(int strike, String type, double ltp, double oi_chg) {
            this.strike = strike;
            this.type = type;
            this.ltp = ltp;
            this.oi_chg = oi_chg;
        }
    }

    public static class MarketProfileData {
        public double vah;
        public double val;
        public double poc;

        public MarketProfileData(AuctionProfileCalculator.MarketProfile profile) {
            this.vah = profile.getVah();
            this.val = profile.getVal();
            this.poc = profile.getPoc();
        }
    }

    public static DashboardEvent from(VolumeBar bar, SignalEngine.AuctionState state, AuctionProfileCalculator.MarketProfile profile) {
        DashboardEvent event = new DashboardEvent();
        event.timestamp = bar.getStartTime();
        event.spot = bar.getClose();
        event.future = bar.getClose() + 15.0; // Placeholder
        event.weighted_delta = bar.getCumulativeVolumeDelta(); // Placeholder using delta
        event.auction_state = state.toString();
        event.pcr = 1.1; // Placeholder
        event.theta_guard_sec = 1200; // Placeholder

        // Mock data for placeholders
        event.heavyweights = List.of(
            new Heavyweight("RELIANCE", 3353, "10.2%"),
            new Heavyweight("HDFC BANK", 1690, "9.1%")
        );
        event.option_window = List.of(
            new Option(24600, "CE", 114.78, 5.2),
            new Option(24650, "CE", 79.57, 12.5),
            new Option(24650, "PE", 81.56, -2.1)
        );

        if (profile != null) {
            event.market_profile = new MarketProfileData(profile);
        }

        return event;
    }
}
