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

### Complete JSON example 

```json

{
	"feeds": {
		"NSE_INDEX|Nifty Bank": {
			"fullFeed": {
				"indexFF": {
					"ltpc": {
						"ltp": 59776.15,
						"ltt": "1764226006000",
						"cp": 59528.05
					},
					"marketOHLC": {
						"ohlc": [
							{
								"interval": "1d",
								"open": 59605.3,
								"high": 59866.6,
								"low": 59549.45,
								"close": 59776.15,
								"ts": "1764181800000"
							},
							{
								"interval": "I1",
								"open": 59758.2,
								"high": 59774.85,
								"low": 59756.8,
								"close": 59769.9,
								"ts": "1764225900000"
							}
						]
					}
				}
			},
			"requestMode": "full_d5"
		},
		"NSE_INDEX|Nifty 50": {
			"fullFeed": {
				"indexFF": {
					"ltpc": {
						"ltp": 26244.1,
						"ltt": "1764226006000",
						"cp": 26205.3
					},
					"marketOHLC": {
						"ohlc": [
							{
								"interval": "1d",
								"open": 26261.25,
								"high": 26310.45,
								"low": 26208.9,
								"close": 26244.1,
								"ts": "1764181800000"
							},
							{
								"interval": "I1",
								"open": 26245,
								"high": 26247.7,
								"low": 26243.35,
								"close": 26245.35,
								"ts": "1764225900000"
							}
						]
					}
				}
			},
			"requestMode": "full_d5"
		},
		"NSE_EQ|INE467B01029": {
			"fullFeed": {
				"marketFF": {
					"ltpc": {
						"ltp": 3145.3,
						"ltt": "1764226004253",
						"ltq": "1",
						"cp": 3162.9
					},
					"marketLevel": {
						"bidAskQuote": [
							{
								"bidQ": "26",
								"bidP": 3144.5,
								"askQ": "35",
								"askP": 3145.3
							},
							{
								"bidQ": "17",
								"bidP": 3144.4,
								"askQ": "36",
								"askP": 3145.7
							},
							{
								"bidQ": "45",
								"bidP": 3144.3,
								"askQ": "58",
								"askP": 3145.8
							},
							{
								"bidQ": "153",
								"bidP": 3144.2,
								"askQ": "38",
								"askP": 3145.9
							},
							{
								"bidQ": "239",
								"bidP": 3144.1,
								"askQ": "97",
								"askP": 3146
							}
						]
					},
					"optionGreeks": {},
					"marketOHLC": {
						"ohlc": [
							{
								"interval": "1d",
								"open": 3180,
								"high": 3180,
								"low": 3144,
								"close": 3145.3,
								"vol": "1092756",
								"ts": "1764181800000"
							},
							{
								"interval": "I1",
								"open": 3145.7,
								"high": 3145.7,
								"low": 3144.7,
								"close": 3145.3,
								"vol": "2793",
								"ts": "1764225900000"
							}
						]
					},
					"atp": 3158.71,
					"vtt": "1092756",
					"tbq": 116408,
					"tsq": 260313
				}
			},
			"requestMode": "full_d5"
		},
		"NSE_EQ|INE020B01018": {
			"fullFeed": {
				"marketFF": {
					"ltpc": {
						"ltp": 361.8,
						"ltt": "1764226006640",
						"ltq": "1",
						"cp": 356.4
					},
					"marketLevel": {
						"bidAskQuote": [
							{
								"bidQ": "25",
								"bidP": 361.7,
								"askQ": "432",
								"askP": 361.8
							},
							{
								"bidQ": "846",
								"bidP": 361.65,
								"askQ": "1738",
								"askP": 361.85
							},
							{
								"bidQ": "1340",
								"bidP": 361.6,
								"askQ": "594",
								"askP": 361.9
							},
							{
								"bidQ": "3321",
								"bidP": 361.55,
								"askQ": "5399",
								"askP": 361.95
							},
							{
								"bidQ": "4282",
								"bidP": 361.5,
								"askQ": "2100",
								"askP": 362
							}
						]
					},
					"optionGreeks": {},
					"marketOHLC": {
						"ohlc": [
							{
								"interval": "1d",
								"open": 356.4,
								"high": 362.9,
								"low": 356,
								"close": 361.8,
								"vol": "2422294",
								"ts": "1764181800000"
							},
							{
								"interval": "I1",
								"open": 361.7,
								"high": 361.7,
								"low": 361.65,
								"close": 361.7,
								"vol": "2677",
								"ts": "1764225900000"
							}
						]
					},
					"atp": 360.94,
					"vtt": "2422294",
					"tbq": 849870,
					"tsq": 863503
				}
			},
			"requestMode": "full_d5"
		},
		"NSE_FO|46803": {
			"fullFeed": {
				"marketFF": {
					"ltpc": {
						"ltp": 116.7,
						"ltt": "1764229716810",
						"ltq": "75",
						"cp": 146.4
					},
					"marketLevel": {
						"bidAskQuote": [
							{
								"bidQ": "975",
								"bidP": 116.4,
								"askQ": "675",
								"askP": 116.6
							},
							{
								"bidQ": "2175",
								"bidP": 116.35,
								"askQ": "750",
								"askP": 116.65
							},
							{
								"bidQ": "2100",
								"bidP": 116.3,
								"askQ": "2475",
								"askP": 116.7
							},
							{
								"bidQ": "2100",
								"bidP": 116.25,
								"askQ": "2100",
								"askP": 116.75
							},
							{
								"bidQ": "1875",
								"bidP": 116.2,
								"askQ": "4125",
								"askP": 116.8
							}
						]
					},
					"optionGreeks": {
						"delta": 0.5202,
						"theta": -10.7668,
						"gamma": 0.0014,
						"vega": 12.3347,
						"rho": 1.8859
					},
					"marketOHLC": {
						"ohlc": [
							{
								"interval": "1d",
								"open": 170,
								"high": 208.3,
								"low": 106.85,
								"close": 116.7,
								"vol": "125945775",
								"ts": "1764181800000"
							},
							{
								"interval": "I1",
								"open": 113.4,
								"high": 116.2,
								"low": 113.3,
								"close": 115.75,
								"vol": "1166700",
								"ts": "1764229620000"
							}
						]
					},
					"atp": 166.8,
					"vtt": "125958375",
					"oi": 10136475,
					"iv": 0.0888824462890625,
					"tbq": 3282300,
					"tsq": 1550550
				}
			},
			"requestMode": "full_d5"
		}
	},
	"currentTs": "1764226006839"
}
```


