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

# How to Run

## Prerequisites

Before you begin, ensure you have the following installed and configured:

1.  **Java 21+** and **Maven**.
2.  **Node.js** and **npm**.
3.  **Create a Configuration File:** If you haven't already, create your personal configuration file by copying the example:
    ```bash
    cp ats-core/src/main/resources/config.properties.example ats-core/src/main/resources/config.properties
    ```
    *Note: This `config.properties` file is included in `.gitignore` and should not be committed to version control.*
4.  **WebSocket Port:** The backend WebSocket server runs on port `7070`. Ensure this port is free on your machine. The frontend is pre-configured to connect to this port.

---

## 1. Configuration

Before running the application, you must first create your own `config.properties` file. A template is provided in `ats-core/src/main/resources/config.properties.example`.

Copy this file to `config.properties` in the same directory:

```bash
cp ats-core/src/main/resources/config.properties.example ats-core/src/main/resources/config.properties
```

Then, you can edit your new `config.properties` file to set your configuration.

```properties
# Application Run Mode: "live" or "simulation"
run.mode=simulation

# Upstox API Access Token (only required for "live" mode)
upstox.accessToken=YOUR_ACCESS_TOKEN_HERE

# QuestDB Integration
questdb.enabled=false

# Dashboard UI
dashboard.enabled=true

# Simulation event delay in milliseconds
simulation.event.delay.ms=10

# Replay source: "sample_data" (for now)
replay.source=sample_data
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
*   `simulation.event.delay.ms`: The delay in milliseconds between each event in simulation mode. This is useful for slowing down the replay to a more realistic speed.
*   `replay.source`: The source of data for the simulation. Currently, only `sample_data` is supported, which uses the generated data file.

---

## 2. Running Modes

### 1. Simulation / Backtest Mode

This mode allows you to replay historical or generated data for testing and analysis without connecting to a live data feed. The frontend for this mode is the JULES-HF-ATS dashboard you just built.

#### Step 1: Configure for Simulation

In your `ats-core/src/main/resources/config.properties` file, set the `run.mode` property:

```properties
run.mode=simulation
```

#### Step 2: Ensure Sample Data Exists

The simulation mode relies on a sample data file. You can generate one using the provided script:

```bash
python3 scripts/generate_data.py
```

This will create the necessary `generated_data.json.gz` file in the `ats-core/src/main/resources/data/` directory.

#### Step 3: Run the Backend

1.  **Build the entire project** from the root directory:
    ```bash
    mvn clean install
    ```
2.  **Run the application JAR**. The backend will start, process the entire simulation data file, and keep the WebSocket server running to serve the results to the UI.
    ```bash
    java -jar ats-dashboard/target/ats-dashboard-1.0-SNAPSHOT-jar-with-dependencies.jar
    ```

#### Step 4: Run the Frontend

1.  In a **new terminal**, navigate to the frontend directory:
    ```bash
    cd ats-dashboard/frontend
    ```
2.  **Start the Vite development server**:
    ```bash
    npm run dev
    ```
3.  Open your browser and navigate to **`http://localhost:5173`** to view the dashboard.

---

### 2. Live Mode

This mode connects to a live data feed (e.g., Upstox) for real-time trading and monitoring.

#### Step 1: Configure for Live Mode

1.  In your `ats-core/src/main/resources/config.properties` file, set the `run.mode` property:
    ```properties
    run.mode=live
    ```
2.  **Add your live API credentials** to the same `config.properties` file. For example, you would need to provide your Upstox access token.
    ```properties
    # Example for Upstox
    upstox.accessToken=YOUR_ACCESS_TOKEN_HERE
    ```

#### Step 2: Run the Backend

1.  **Build the project**:
    ```bash
    mvn clean install
    ```
2.  **Run the application JAR**. The backend will initialize the connection to the live data feed and start the WebSocket server.
    ```bash
    java -jar ats-dashboard/target/ats-dashboard-1.0-SNAPSHOT-jar-with-dependencies.jar
    ```

#### Step 3: Run the Frontend

The process is the same as in Simulation mode.

1.  In a **new terminal**, navigate to the frontend directory:
    ```bash
    cd ats-dashboard/frontend
    ```
2.  **Start the Vite development server**:
    ```bash
    npm run dev
    ```
3.  Open your browser and navigate to **`http://localhost:5173`** to view the live dashboard.

---

# Market Data JSON Format

The application expects a gzipped JSON file containing a single JSON array. Each element in the array represents a market data tick and should follow the structure outlined below.

## Tick Object Structure

Each tick object has a single key, `"feeds"`, which contains a nested object.

```json
{
  "feeds": {
    "INSTRUMENT_KEY": {
      "fullFeed": {
        "marketFF": {
          "ltpc": {
            "ltp": 100.0,
            "ltq": "50",
            "ltt": "1672531199000",
            "cp": 95.5
          },
          "marketLevel": {
            "bidAskQuote": [
              {
                "bidP": 99.95,
                "askP": 100.05
              }
            ]
          },
          "tbq": 5000,
          "tsq": 4500,
          "vtt": "1000000",
          "atp": 99.8,
          "oi": 10000
        }
      }
    }
  }
}
```

### Key Descriptions

*   `"feeds"`: The root object.
*   `"INSTRUMENT_KEY"`: A dynamic key representing the instrument's unique identifier (e.g., `"NSE_EQ_RELIANCE"`).
*   `"fullFeed"`: Contains the full market data feed.
*   `"marketFF"`: Contains the market data.
    *   `"ltpc"`: Last Traded Price and Quantity.
        *   `"ltp"` (number): The last traded price.
        *   `"ltq"` (string): The last traded quantity.
        *   `"ltt"` (string): The last traded time in milliseconds since the Unix epoch.
        *   `"cp"` (number): The closing price.
    *   `"marketLevel"`: Contains the bid and ask prices.
        *   `"bidAskQuote"`: An array of bid/ask objects. The application only uses the first element.
            *   `"bidP"` (number): The best bid price.
            *   `"askP"` (number): The best ask price.
    *   `"tbq"` (number): The total buy quantity.
    *   `"tsq"` (number): The total sell quantity.
    *   `"vtt"` (string): The volume traded today.
    *   `"atp"` (number): The average traded price.
    *   `"oi"` (number): The open interest.

### Minimal Example

Here is the absolute minimum JSON structure required for the application to process a tick:

```json
{
  "feeds": {
    "NSE_EQ_TEST": {
      "fullFeed": {
        "marketFF": {
          "ltpc": {
            "ltp": 100.0,
            "ltq": "1",
            "ltt": "1672531200000",
            "cp": 100.0
          },
          "marketLevel": {
            "bidAskQuote": [
              {
                "bidP": 99.95,
                "askP": 100.05
              }
            ]
          }
        }
      }
    }
  }
}
```
