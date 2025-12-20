# Next Steps

This document outlines the remaining tasks for the HF-ATS project.

## 1. Implement Market Data Publishing

The `UpstoxMarketDataStreamer` currently connects to the Upstox API but does not publish the received market data to the LMAX Disruptor. The `onUpdate` and `publishMarketUpdate` methods are commented out.

**Tasks:**

*   Uncomment and fully implement the `onUpdate` method to process incoming `MarketUpdateV3` objects.
*   Uncomment and complete the `publishMarketUpdate` method to map the `MarketUpdateV3` data to the `MarketEvent` POJO and publish it to the Disruptor's Ring Buffer.
*   Implement the logic in the `onUpdate` method to call the `DynamicStrikeSubscriber` when the Nifty Spot price changes.

## 2. Verify and Complete Core Components

Several core components are already in place but need to be verified and potentially completed.

**Tasks:**

*   **QuestDBWriter:** Verify that the `QuestDBWriter` is correctly persisting `MarketEvent` data to the QuestDB instance. This includes checking the table schema and ensuring that all fields are being written correctly.
*   **VolumeBarGenerator:** Test the `VolumeBarGenerator` to ensure that it is correctly aggregating ticks into volume-based bars and that the `AuctionEvent` is being triggered at the appropriate time.
*   **IndexWeightCalculator:** Verify that the `IndexWeightCalculator` is correctly calculating the weighted index delta based on the incoming tick data.
*   **DynamicStrikeSubscriber:** Test the `DynamicStrikeSubscriber` to ensure that it is correctly identifying the new ATM strikes and that the subscription and unsubscription logic is working as expected.

## 3. Implement "Theta-Exit" Guard

The "Theta-Exit" guard is a critical component of the trading logic that is not yet implemented.

**Tasks:**

*   Extend the `MarketEvent` to include the necessary option greeks (Theta, Delta, etc.).
*   Create a `ThetaExitGuard` class that consumes `MarketEvent`s from the Disruptor.
*   Implement the logic to monitor the time decay of option positions and trigger liquidation orders when necessary.

## 4. Enhance Order Management

The `UpstoxOrderManager` needs to be extended to support order modifications and cancellations.

**Tasks:**

*   Add `modifyOrder` and `cancelOrder` methods to the `UpstoxOrderManager`.
*   Implement the underlying logic using the appropriate methods from the Upstox Java SDK.

## 5. Performance Tuning and Optimization

After the core features are in place, the system will require a thorough performance analysis.

**Tasks:**

*   Profile the application to identify any performance bottlenecks.
*   Optimize the Disruptor configuration and event handlers for maximum throughput and low latency.
*   Investigate off-heap memory storage for the `MarketEvent` objects to reduce GC pressure.
