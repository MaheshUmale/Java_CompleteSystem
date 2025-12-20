# Next Steps

This document outlines the remaining tasks for the HF-ATS project.

## 1. Resolve Backtesting Environment Issue

The immediate priority is to resolve the issue preventing the `MarketDataReplayer` from successfully querying the QuestDB database. The application is consistently failing with a `PSQLException: ERROR: Invalid column: ts`.

**Next Actions:**

*   Investigate the JDBC driver version and its compatibility with the QuestDB instance.
*   Verify the exact timestamp format expected by the QuestDB instance running in the environment.
*   Explore alternative query methods or JDBC settings to resolve the parsing issue.

## 2. Implement "Theta-Exit" Guard

Once the backtesting environment is stable, the next major feature to implement is the "Theta-Exit" guard.

**Tasks:**

*   Extend the `MarketEvent` to include option greeks (Theta, Delta, etc.).
*   Create a `ThetaExitGuard` class that consumes `MarketEvent`s from the Disruptor.
*   Implement the logic to monitor the time decay of option positions and trigger liquidation orders when necessary.

## 3. Enhance Order Management

The `UpstoxOrderManager` needs to be extended to support order modifications and cancellations.

**Tasks:**

*   Add `modifyOrder` and `cancelOrder` methods to the `UpstoxOrderManager`.
*   Implement the underlying logic using the appropriate methods from the Upstox Java SDK.

## 4. Performance Tuning and Optimization

After the core features are in place, the system will require a thorough performance analysis.

**Tasks:**

*   Profile the application to identify any performance bottlenecks.
*   Optimize the Disruptor configuration and event handlers for maximum throughput and low latency.
*   Investigate off-heap memory storage for the `MarketEvent` objects to reduce GC pressure.
