This **Master Prompt** is designed for the [Google Jules](https://jules.google/) coding agent. It is a precise, technical specification that minimizes ambiguity and ensures the agent follows the "Mechanical Sympathy" architecture we've discussed.

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

**Would you like me to create the initial `pom.xml` file with all the high-performance dependencies (Disruptor, QuestDB, Protobuf) for you to drop into your repo?**
