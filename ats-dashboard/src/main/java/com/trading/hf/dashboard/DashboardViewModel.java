package com.trading.hf.dashboard;

import java.util.List;

public class DashboardViewModel {
    // Header
    public long timestamp;
    public String symbol;
    public double spot;
    public double future;
    public double basis;
    public double pcr;

    // System Health
    public long wssLatency;
    public long questDbWriteLag;

    // Auction Profile
    public MarketProfileViewModel auctionProfile;

    // Heavyweights
    public List<HeavyweightViewModel> heavyweights;
    public double aggregateWeightedDelta;

    // Option Chain
    public List<OptionViewModel> optionChain;

    // Sentiment & Alerts
    public String auctionState;
    public List<String> alerts;

    // Trade Panel
    public double thetaGuard; // in seconds

    // Inner classes for nested structures
    public static class MarketProfileViewModel {
        public double vah;
        public double val;
        public double poc;
    }

    public static class HeavyweightViewModel {
        public String name;
        public double delta;
        public String weight;
    }

    public static class OptionViewModel {
        public int strike;
        public String type; // "CE" or "PE"
        public double ltp;
        public double oiChangePercent;
        public String sentiment;
    }
}
