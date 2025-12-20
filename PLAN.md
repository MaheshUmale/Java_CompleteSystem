
Here is the **15-Step Functional Requirement Document** for the **JULES-HF-ATS**.
DO ADDITIONAL RESEARCH IF NEEDED 
---

### **Section 1: Overall System Topography**

#### **Step 1: The Multi-Threaded Ingestion Hub**

The system establishes a persistent WebSocket connection to Upstox v3.

* **Data Inputs:** Binary Protobuf stream for 250+ instruments (Nifty 50 stocks + Nifty Spot + Option Chain).
* **Interface:** `UpstoxStreamListener` (Producer).
* **Action:** Decodes `LTP`, `Size`, `Bid/Ask`, and `OI` at the packet level.

#### **Step 2: The LMAX Disruptor Ring Buffer (The Central Nervous System)**

A lock-free messaging backbone that multicasts decoded events.

* **Rationale:** Eliminates thread contention by allowing multiple consumers (DB, Weights, Strategy) to read the same data packet simultaneously.
* **Interface:** `Disruptor<MarketEvent>`.

#### **Step 3: Persistence via QuestDB ILP**

* **Action:** Immediate storage of every raw tick.
* **Requirement:** Use Influx Line Protocol (ILP) for zero-latency write operations.
* **Data stored:** `(Symbol, Price, Volume, Side, OBI, Timestamp)`.

---

### **Section 2: The Logic Engines (The Pre-Frontal Cortex)**

#### **Step 4: The Index Weightage Engine (Nifty50/BankNifty)**

* **Data:** A static JSON map of NSE weights (e.g., HDFCBANK: 13.5%, RELIANCE: 9.1%).
* **Calculation:** For every tick in a heavyweight stock, calculate:


* **Interface:** `IndexSentimentProvider`.

#### **Step 5: Dynamic ATM ± 2 Strike Manager**

* **Action:** Tracks Nifty Spot price. If Spot moves  points, it triggers an API call to Upstox to update the subscription.
* **Buffer:** Keeps "expired" ATM strikes for 5 minutes to allow open orders to exit smoothly.

#### **Step 6: Volume-Based Bar Aggregator**

* **Input:** Raw Ticks.
* **Action:** Ignores time. A bar is "closed" only when .
* **Output:** `VolumeBar(Open, High, Low, Close, Delta, POC)`.

---

### **Section 3: Auction Market Theory (The Brain)**

#### **Step 7: Value Area (VA) & POC Calculation**

* **Methodology:** Uses the 70% distribution rule.
* **Logic:** Starts at the **Point of Control (POC)** and expands outward until 70% of the session volume is contained.
* **Interface:** `AuctionProfileRegistry`.

#### **Step 8: Imbalance & Initiative Detection**

* **The "Initiative" Signal:** If Price breaks above **Value Area High (VAH)** AND **Weighted Index Delta** is positive.
* **The "Absorption" Signal:** High Volume at VAH but Price fails to break out (Delta Divergence).

#### **Step 9: Option Chain "Change in OI" Monitor**

* **Logic:** Tracks the rate of change in Open Interest (OI) for ATM  strikes.
* **Significance:** Rapid increase in Put OI + Rising Index Delta = **Strong Floor Migration**.

#### **Step 10: The Theta-Exit Guard**

* **Calculated Field:** `Current_Premium - (Theta_Value \times Minutes_Held)`.
* **Action:** Kills the trade if "Price Velocity" does not outpace "Time Decay."

---

### **Section 4: The Wiring & Integration (Wiring the Brain)**

#### **Step 11: The "Auction State" Machine**

The Brain maintains a global state for the Index:

1. **ROTATION:** Price between VAL and VAH.
2. **DISCOVERY:** Price breaking VA extremes with high Delta.
3. **REJECTION:** Price breaks VA but OBI (Order Book Imbalance) reverses.

#### **Step 12: Interface Contract: `SignalEvent**`

The Brain emits a `SignalEvent` containing:

* `Direction` (Long/Short)
* `ConfidenceScore` (Based on Weightage + Delta + OBI)
* `Instrument` (The specific ATM Call/Put)

#### **Step 13: Order Execution & Slippage Engine**

* **Action:** Converts `SignalEvent` into a `LimitOrder` placed at the `BestAsk + 0.05`.
* **Backtest Simulation:** Uses raw ticks to simulate fills at the actual historical Bid/Ask, not the LTP.

#### **Step 14: Monitoring & Health Dashboard**

* **Metrics:** WSS Latency, QuestDB WAL Lag, Disruptor Buffer Fill Rate.
* **Visuals:** Real-time Volume Profile vs. Price.

#### **Step 15: The Final JULES Prompt Interface**

Jules is instructed to build these as **Separated Concerns**. If the `QuestDBWriter` crashes, the `SignalEngine` must continue to process live trades from the Disruptor.

---

### **Summary Table: Data to Calculation Matrix**

| Data Feed | Calculation / Logic | Output To |
| --- | --- | --- |
| **Stock Ticks** | Tick  Weight | **Weighted Index Delta** |
| **Spot Price** | Round to nearest 50 | **Dynamic Strike Selector** |
| **Option Ticks** |  | **Support/Resistance Strength** |
| **Raw Volume** | Cumulative Sum | **Volume-Based Bar Generator** |

[LMAX Disruptor and Low Latency Concepts](https://www.youtube.com/watch?v=Qho1QNbXBso)

This video explains the "Mechanical Sympathy" required to build the high-speed Java messaging backbone we've designed, ensuring your 250+ instruments are processed without thread-locking.
