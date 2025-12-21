# High-Frequency Auction Trading System (HF-ATS)

This project is a high-performance trading application built in Java 21. It is designed to execute strategies based on **Auction Market Theory (AMT)** by analyzing market pressure at the tick level. The system focuses on **Volume-based candles** and **Order Book Imbalance (OBI)** rather than traditional time-based analysis.

## Key Technologies

*   **Language:** Java 21 (LTS) with Project Loom for virtual threads.
*   **Core Architecture:** LMAX Disruptor for lock-free, high-throughput messaging.
*   **Data Ingestion:** Upstox API v3 (Protobuf/WebSocket) for live market data.
*   **Persistence:** QuestDB via the Influx Line Protocol (ILP) for high-speed tick data storage.
*   **Dashboard:** Javalin web server with WebSockets and ApexCharts for a real-time, web-based UI.

## Architecture Overview

The system is built as a multi-module Maven project to ensure a clean separation of concerns:

*   `ats-core`: This module contains the core business logic, including the data ingestion pipeline, the LMAX Disruptor, the volume bar generator, and the persistence layer.
*   `ats-dashboard`: This is an optional module that provides a real-time, web-based dashboard to visualize the data being processed by the core engine.

The application is event-driven. The `UpstoxMarketDataStreamer` (or the `SampleDataReplayer` in simulation mode) acts as the producer, publishing market data events to the LMAX Disruptor. These events are then consumed by multiple parallel consumers, including the `VolumeBarGenerator`, `IndexWeightCalculator`, and the `QuestDBWriter`.

---

## How to Run

### 1. Configuration

Before running the application, you must configure it by editing the `config.properties` file located in `ats-core/src/main/resources/`.

```properties
# Application Run Mode: "live" or "simulation"
run.mode=simulation

# Upstox API Access Token (only required for "live" mode)
upstox.accessToken=YOUR_ACCESS_TOKEN_HERE

# QuestDB Integration
questdb.enabled=false

# Dashboard UI
dashboard.enabled=true
```

**Configuration Flags:**

*   `run.mode`:
    *   `simulation`: Runs the application offline using the provided sample data. This is the default.
    *   `live`: Connects to the Upstox API for live market data. Requires a valid `upstox.accessToken`.
*   `questdb.enabled`:
    *   `true`: Enables the `QuestDBWriter` to persist all incoming tick data. Requires a running QuestDB instance.
    *   `false`: Disables the QuestDB integration.
*   `dashboard.enabled`:
    *   `true`: Starts the web-based dashboard, accessible at `http://localhost:7070`.
    *   `false`: Disables the dashboard.
*   `upstox.accessToken`: Your personal access token for the Upstox API.

### 2. Build the Application

The project is built using Apache Maven. To build the executable JAR, run the following command from the project's root directory:

```bash
mvn clean install
```

This will compile the code and create a self-contained, executable JAR file in the `ats-dashboard/target/` directory.

### 3. Run the Application

Once the build is complete, you can run the application using the following command:

```bash
java -jar ats-dashboard/target/ats-dashboard-1.0-SNAPSHOT-jar-with-dependencies.jar
```

The application will start in the mode specified in your `config.properties` file.

### 4. Access the Dashboard

If you have `dashboard.enabled=true` in your configuration, you can access the dashboard by opening a web browser and navigating to:

**http://localhost:7070**

You will see a real-time chart of the volume bars and a display of the latest Order Book Imbalance as the data is processed.

---

## Core Algorithmic Logic

*   **Volume Bar Factory:** Aggregates raw ticks into bars defined by **Volume (Quantity)**, not time. A new bar is created only when a certain volume threshold is met.
*   **Aggressor Identification:** The side of a trade (buy or sell) is determined by comparing the last trade price (LTP) to the best bid and ask prices. If `LTP >= Ask`, it is considered an aggressive buy. If `LTP <= Bid`, it is an aggressive sell. This is crucial for calculating Cumulative Volume Delta (CVD).
*   **Order Book Imbalance (OBI):** Calculated on every tick to measure the relative buy and sell pressure in the order book.
