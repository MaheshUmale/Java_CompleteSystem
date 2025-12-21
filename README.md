This document serves as the **Single Source of Truth** for **Google Jules** to build your High-Frequency Auction Trading System (HF-ATS). It combines your 20-year architectural vision with the high-performance capabilities of the Java 21 ecosystem.
---

# HF-ATS: System Requirements & Architectural Specification

## 1. Core Mission & Rationale

The HF-ATS is designed to exploit **Auction Market Theory (AMT)** by analyzing market pressure at the tick level. Unlike traditional retail bots, this system treats **Time as a secondary dimension**, focusing instead on **Volume/Tick-based candles**, **Order Book Imbalance (OBI)**, and **Option Greek Decay (Theta)**.

* **Language:** Java 21 (LTS) — selected for Project Loom (Virtual Threads) and high-performance profiling.
* **Infrastructure:** LMAX Disruptor (Lock-free messaging) + QuestDB (High-speed tick storage).
* **API:** Upstox v3 (Protobuf/WebSocket).

---

## 2. Technical Architecture & Component Design

### A. The Ingestion Engine (Producer)

* **Mechanism:** Single-threaded WebSocket listener using **Project Loom** for non-blocking I/O.
* **Protocol:** Upstox v3 Binary Protobuf.
* **Key Task:** Decode `marketDataFeed.proto` and wrap every tick into a pre-allocated `MarketEvent` POJO.
* **Jules Instruction:** "Use `java.net.http.WebSocket` with a custom `Listener`. Avoid thread-pooling here; the listener must immediately hand off to the Disruptor."

### B. The Messaging Backbone (LMAX Disruptor)

* **Configuration:** Single Producer, Multiple Consumers.
* **Ring Buffer Size:**  (65,536) slots to handle burst volatility without backpressure.
* **Wait Strategy:** `YieldingWaitStrategy` for sub-millisecond latency.

### C. Persistence Tier (QuestDB + ILP)

* **Protocol:** InfluxDB Line Protocol (ILP) via TCP.
* **Table Schema:**
```sql
CREATE TABLE ticks (
    ts TIMESTAMP, symbol SYMBOL, ltp DOUBLE, size LONG,
    side INT, tbq LONG, tsq LONG, delta DOUBLE
) timestamp(ts) PARTITION BY DAY WAL;

```


* **Jules Instruction:** "Use the `questdb-java-client`. Implement an async flush: commit every 10,000 ticks or 1 second to prevent WAL lag."

---

## 3. Algorithmic Intelligence (The "Brain")

### I. Volume Bar Factory

* **Logic:** Aggregates raw ticks into bars defined by **Volume (Quantity)**, not time.
* **State Management:** Maintain a `Map<String, RunningBar>` for 250 instruments.
* **Trigger:** When `currentVolume >= threshold`, finalize the bar and emit an `AuctionEvent`.

### II. The "Theta-Exit" Guard

* **Rationale:** Prevent premium erosion during sideways "silent" auctions.
* **Logic:**


* **Action:** If price doesn't compensate for decay, the position is liquidated automatically.

### III. Dynamic ATM Handover

* **Logic:** Monitor the **Nifty Spot** tick.
* **Threshold:** Every  point move.
* **Task:** Unsubscribe from  strikes; Subscribe to  strikes.
* **Safety:** Maintain a "Grace Period" where old strikes are tracked for 120 seconds post-handover to allow for active exit orders to fill.

---

## 4. Backtesting & Replay Engine

* **Requirement:** The `StrategyEngine` must be interface-driven.
* `LiveFeedProvider` (WebSocket)
* `HistoricalReplayProvider` (QuestDB Query)


* **Slippage Model:** Backtests must execute at the `AskP` (for Buys) and `BidP` (for Sells) found in the `marketLevel` data, simulating a 50ms execution delay.

---

## 5. Dashboard: The "Auction Command Center"

Instead of price charts, the UI (built via JavaFX or a Web TUI) must show:

1. **Pressure Gauges:** Real-time OBI for Nifty/BankNifty.
2. **Strike Heatmap:** Where Put/Call walls are shifting.
3. **Weighted Sector Heatmap:** Performance of the Top 10 heavyweights (Reliance, HDFC, etc.) contributing to Index movement.

---

## 6. Development Roadmap for JULES (Sequential)

1. **Phase 1 (The Foundation):** Build the Maven project, integrate the Upstox `.proto` file, and implement the `ProtobufDecoder`.
2. **Phase 2 (The Flow):** Setup the LMAX Disruptor Ring Buffer and the `QuestDBWriter` using ILP.
3. **Phase 3 (The Logic):** Implement the `VolumeBarGenerator` and the `IndexWeightCalculator`.
4. **Phase 4 (The Execution):** Build the `OrderManager` using Upstox v3 Order APIs with **OAuth2** authentication.
5. **Phase 5 (Verification):** Build a basic "Tick-to-Bar" CLI to verify that ticks are being captured and aggregated without loss.

---

## 7. Reference Material for JULES

* **LMAX Disruptor v4.0:** Focus on lock-free `SequenceBarrier` for multicasting.
* **QuestDB ILP Docs:** Use "Automatic Table Creation" via ILP for flexibility.
* **Upstox API v3 Documentation:** [Order Placement V3](https://upstox.com/developer/api-documentation/v3/place-order/) and [Market Data Feed V3](https://upstox.com/developer/api-documentation/v3/get-market-data-feed/).

---
 ---

# NFRs


#### **Requirement 1: Non-Blocking Data Ingestion**

* Implement an **LMAX Disruptor (v4.0)** Ring Buffer.
* The `UpstoxProtobufListener` (Producer) must decode binary packets and publish to the Ring Buffer in `< 10 microseconds`.
* Use **Java 21 Virtual Threads** to manage the heartbeat and connection monitoring of the WebSocket.

#### **Requirement 2: State-Aware Volume Bar Factory**

* Create a `VolumeBarGenerator` that maintains an in-memory "running state" for all 250 instruments.
* Logic: Accumulate `tick_volume`. When `sum >= threshold`, finalize the bar, calculate `OHLCV + Vwap + OBI`, and persist to QuestDB.
* **Crucial:** Use a `ConcurrentHashMap` or a lock-free array to store instrument states to avoid thread contention.

#### **Requirement 3: The "Auction Market Theory" Signal Engine**

* **OBI (Order Book Imbalance):** Calculate every tick using `tbq` and `tsq`.
* **CVD (Cumulative Volume Delta):** Track buy-side vs. sell-side aggression.
* **Dynamic Handover:** When Nifty Spot moves 50 points, the system must unsub from the old "OOM" strikes and sub to the new "ATM" strikes without dropping the main feed.

#### **Requirement 4: QuestDB Stability Guardrails**

* Mandatory use of the **QuestDB Java ILP Client** (TCP transport).
* Table configuration: `WAL Enabled`, `Partition by DAY`.
* Implement an **Async Flush Strategy**: Commit every 1,000 rows or 500ms (whichever is first) to prevent WAL lag.

#### **Requirement 5: Backtesting Replay Engine**


## Additonal info :
MarketDataFeed.proto latest file is V3 and you may not need  file directly

https://github.com/upstox/upstox-java
THIS PACKAGE HAVE needed file with implementation of client ...
https://github.com/upstox/upstox-java/tree/master/examples/websocket/market_data/v3

Still if you need the file location is as below

https://assets.upstox.com/feed/market-data-feed/v3/MarketDataFeed.proto

* Implement a `MarketDataReplayer` that queries QuestDB for raw ticks and pushes them through the same Disruptor used in live trading.
* The Strategy Engine must be **"Source Agnostic"** (it shouldn't know if the data is from WSS or QuestDB).
---
