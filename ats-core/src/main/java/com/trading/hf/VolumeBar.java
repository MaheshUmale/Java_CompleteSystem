package com.trading.hf;

public class VolumeBar {
    private final String symbol;
    private final long volumeThreshold;
    private double open;
    private double high;
    private double low;
    private double close;
    private long volume;
    private long startTime;
    private long endTime;
    private boolean isClosed;
    private long cumulativeVolumeDelta;

    public VolumeBar(String symbol, long volumeThreshold) {
        this.symbol = symbol;
        this.volumeThreshold = volumeThreshold;
    }

    public void addTick(long timestamp, double price, long quantity, long signedVolume) {
        if (startTime == 0) {
            open = price;
            high = price;
            low = price;
            startTime = timestamp;
        }
        high = Math.max(high, price);
        low = Math.min(low, price);
        close = price;
        volume += quantity;
        cumulativeVolumeDelta += signedVolume;
        endTime = timestamp;

        if (volume >= volumeThreshold) {
            isClosed = true;
        }
    }

    public boolean isClosed() {
        return isClosed;
    }

    public String getSymbol() {
        return symbol;
    }

    public double getOpen() {
        return open;
    }

    public double getHigh() {
        return high;
    }

    public double getLow() {
        return low;
    }

    public double getClose() {
        return close;
    }

    public long getVolume() {
        return volume;
    }

    public long getCumulativeVolumeDelta() {
        return cumulativeVolumeDelta;
    }
}
