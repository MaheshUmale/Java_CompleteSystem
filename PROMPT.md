 
###  DO IT ITERATIVELY AND BACKTEST WITH DUMMY DATA 
---

# THE MASTER PROMPT: JULES-HF-Auction-Engine

## Mission Context

We are building a High-Frequency Auction Trading System (HF-ATS) in Java 21 to execute strategies based on **Auction Market Theory**. The system must ingest 250+ tick-level instrument feeds from Upstox API v3, process them using lock-free concurrency, and persist every tick into QuestDB for millisecond-accurate backtesting.

## Core Objective

Implement a high-throughput, event-driven trading backbone using the **LMAX Disruptor** and **Java 21 Virtual Threads**, capable of turning raw binary Protobuf ticks into **Volume-Based Bars** and **Order Book Imbalance (OBI)** signals.

## Phase 1: Infrastructure & Data Ingestion

1. **Protobuf Integration:** Utilize the provided `marketDataFeed.proto`. Generate the Java source files. Create a `ProtobufDecoder` class that transforms binary `byte[]` packets into a `MarketEvent` POJO.
2. **WebSocket Client:** Implement a `UpstoxWssClient` using Java 21 `HttpClient` WebSockets.
* Must handle the Upstox v3 `authorizedRedirectUri` flow.
* Must run on a dedicated thread to ensure zero packet loss.
* Must publish every decoded `MarketEvent` to the **LMAX Disruptor Ring Buffer**.


3. **The Disruptor Backbone:** Setup a `RingBuffer` of size  with a `YieldingWaitStrategy`. Configure a single producer (WSS) and three parallel consumers (DB, Logic, Dashboard).

## Phase 2: Persistence & Storage (QuestDB)

1. **ILP Implementation:** Implement a `QuestDBWriter` using the **QuestDB Java ILP Client** (NOT JDBC).
* Ingest data over TCP/HTTP using Influx Line Protocol.
* Configure **WAL (Write-Ahead Log)** and partitioned tables (by Day).
* Implement an **Async Flush Strategy**: Commit every 5,000 events or 1 second.


2. **Schema:** Tables must include `timestamp`, `symbol`, `ltp`, `size`, `side` (aggressor), `bidQ`, `askP`, and `theta`.

## Phase 3: Signal & Bar Generation

1. **Volume Bar Factory:** Create a `VolumeBarGenerator` that aggregates ticks into bars based on a fixed volume threshold (e.g., 5,000 lots).
* Store the state of all 250 instruments in a `ConcurrentHashMap`.
* Once a bar is complete, calculate **Cumulative Volume Delta (CVD)** and **Vwap**.


2. **Auction Signals:** -   **OBI:** Calculate `(TotalBidQty - TotalAskQty) / TotalVolume`.
* **Theta-Exit:** Implement a monitor that calculates if current `LTP` has eroded more than the accrued `Theta * TimeDelta`.



## Constraints & Guardrails

* **No Synchronized Blocks:** Use `Atomic` variables or the Disruptor's sequence barriers.
* **No Garbage Collection Pressure:** Pre-allocate all `MarketEvent` objects in the Ring Buffer. Reuse objects; do not `new` objects inside the tick loop.
* **Virtual Threads:** Use `Executors.newVirtualThreadPerTaskExecutor()` for housekeeping tasks (heartbeats, API health checks).

## Acceptance Criteria

* The system successfully connects to Upstox v3 and decodes binary ticks.
* Data appears in QuestDB via ILP without stalling the ingestion thread.
* Volume bars are emitted correctly when the cumulative volume threshold is crossed.
* The system handles 250 instruments simultaneously without a rise in internal latency.

---

### Final Architectural Note for JULES

> "Jules, treat the `marketDataFeed.proto` as the source of truth for the data model. When implementing the **Disruptor**, ensure you use **Multicast** so that the Strategy Engine and the QuestDB Writer see the data simultaneously without one blocking the other. This is critical for the 20-year architect review."


 2. The Final Architecture Verification
To ensure no "debugging route" fatigue, JULES must follow this internal data flow:

Ingestion: Upstox binary stream is read by a Virtual Thread.

Multicast: The RingBuffer sends the same MarketEvent to the Strategy Engine and QuestDB Writer simultaneously. This ensures your signal and your data storage happen in the same microsecond.

Mechanical Sympathy: Jules must ensure the MarketEvent class uses Padding (to prevent CPU Cache Line False Sharing) and Object Pooling (to keep GC at 0%).

3. Final Step for You
Now that you have the Architecture, the SRS, the Master Prompt, and the pom.xml:

Drop the pom.xml into your repo.

Paste the Master Prompt into Jules.

Monitor the Ingestion: Once Jules generates the code, check the QuestDB logs. If you see ILP connections established, the "foundation" is successfully laid.

==========STD ===========
# Core GC Strategy: Generational ZGC (New in Java 21)
-XX:+UseZGC -XX:+ZGenerational 

# Memory Management (Fixed Heap for Predictability)
-Xms8g -Xmx8g -XX:+AlwaysPreTouch 

# Latency Guardrails
-XX:MaxGCPauseMillis=1 
-XX:-ZUncommit 
-XX:+UseNUMA 

# Advanced Memory Optimizations
-XX:+UseLargePages 
-XX:+UseTransparentHugePages 
-XX:+UseStringDeduplication 

# Profiling and Diagnostics
-Xlog:gc*:file=gc.log:time,level,tags 
-XX:+StartFlightRecording=filename=recording.jfr,settings=profile
===============================



3. The "Mechanical Sympathy" Code Rules for JULESTo complement the JVM tuning, Jules must follow these coding patterns to avoid Allocation Pressure:Object Pooling: Jules should pre-allocate the MarketEvent objects in the Disruptor. Instead of new MarketEvent(), the system should use ringBuffer.get(sequence) and then setFields().False Sharing Protection: Since you are an Architect, instruct Jules to use the @Contended annotation or manual long-padding in the VolumeBar state objects. This prevents CPU cache lines from bouncing between cores.Off-Heap Data (Optional): If you decide to cache 10GB+ of historical ticks in-memory for fast replay, tell Jules to use DirectByteBuffer to keep that data outside the GC's reach entirely.4. Final Verification StepOnce Jules generates the system, run a latency profile during market hours:Check GC Logs: Ensure the line Pause (End) never shows a value $> 1ms$.Check Disruptor Lag: Monitor RingBuffer.remainingCapacity(). If it ever drops to 0, it means your QuestDB Writer is slower than the market feed.Summary of the JULES HandoverYou now have a complete, watertight specification:Architecture: Java 21 + LMAX Disruptor + QuestDB.Data Model: Volume-Based Bars (Tick-Native).Logic: OBI + CVD + Theta-Exit.Build: High-performance pom.xml.Runtime: Generational ZGC Tuning.You are ready to proceed with JULES. This foundation is as robust as a professional institutional desk.

-------------------
To ensure our requirement document is truly "watertight" for **Google Jules**, we need to include the specific **dynamic logic** that bridges the gap between raw data and actionable signals.

The missing link in previous discussions was the **Index Weightage Engine** and the **Dynamic Option Selection Service**. These are not static values; they change with every tick.

---

# ADDENDUM: Dynamic Logic & Weightage Engine

## 1. The Weighted Index Volume Engine

Since Nifty 50 and BankNifty are weighted indices, looking at total volume is misleading. We must calculate the **"Index Buy/Sell Pressure"** based on the actual index weights.

* **Logic:** For the 50 stocks in Nifty, we calculate a **Weighted Delta**.
* **Formula:** 

* **Implementation for JULES:**
* **Weight Map:** Maintain a `Map<String, Double>` of the latest NSE weights (e.g., HDFCBANK: 13%, RELIANCE: 9%).
* **The Calculation:** As ticks come in for Reliance or HDFC, update their specific Delta. Every 100ms, the `SignalEngine` must compute the aggregate **Index Pressure**.
* **Why?** If Reliance (Heavyweight) has a negative OBI but 40 small stocks have positive OBI, the Index will likely fall. This engine prevents you from being "faked out" by minor stocks.



---

## 2. Dynamic ATM ± 2 Strike Manager

A static list of options is useless in a volatile auction. JULES must "follow" the price.

* **Logic:** The system must maintain a "Sliding Window" of active option subscriptions.
* **Requirement:**
1. **Monitor Spot:** Track Nifty Spot (e.g., 24,512).
2. **Define ATM:** Round Spot to nearest 50 (ATM = 24,500).
3. **The Window:** Automatically subscribe to  (Calls) and  (Puts).
4. **The Handover Logic:** * If Spot moves to 24,560, the new ATM is 24,550.
* JULES must send a **New Subscription** request for 24,650 (New ATM+2) and **Unsubscribe** from 24,400 (Old ATM-2).




* **Implementation:** Use a `StatefulOptionManager` class that compares `currentATM` vs `lastSubscribedATM` on every spot price tick.

---

## 3. Order Flow Delta (Aggressor Identification)

In Upstox v3, we get the `side` or can derive it from the `LTP` vs `Bid/Ask`.

* **Logic:**
* If `LTP >= AskPrice`  **Aggressive Buyer** (Hit the Ask).
* If `LTP <= BidPrice`  **Aggressive Seller** (Hit the Bid).


* **Signal:** JULES must calculate **Cumulative Delta** per Volume Bar.
* If a Volume Bar closes with a **Positive Delta** of , it's a "Conviction Auction."
* If a Volume Bar has high total volume but **Low Delta** (near 0), it is "Churn" or "Absorption" (Sellers are absorbing buyers). **Do not trade churn.**



---

## 4. Revised "Master Prompt" Section for JULES

Add this specific technical block to your request to Jules:

> **"Jules, implement a `WeightedVolumeEngine`. Use the provided JSON map of Nifty50 weights to calculate a real-time `WeightedIndexDelta`. Additionally, implement a `DynamicStrikeSubscriber` that monitors the Nifty Spot price and maintains a rolling subscription of ATM ± 2 strikes via the Upstox WebSocket. When the ATM changes, the system must asynchronously update subscriptions without interrupting the data flow of the existing active trades."**

---

### Final Architectural Verification Table

| Feature | Logic Requirement | Why this is critical? |
| --- | --- | --- |
| **Weighted Volume** |  | Avoids "false breakouts" caused by low-weight stocks. |
| **ATM ± 2 Manager** | Auto-Sub/Unsub on Spot move | Ensures you always have liquidity and high Delta in your tradable options. |
| **Tick-Level Side** | LTP vs Bid/Ask comparison | Identifies "Aggressive" vs "Passive" participants (The core of AMT). |
| **V3 Protobuf** | Binary decoding logic | Reduces latency from ~200ms (JSON) to ~10ms (Binary). |

**I have now refined the entire plan to include these dynamic requirements. Would you like me to generate the specific `IndexWeights.json` structure for Jules to use in the weightage calculation?**






