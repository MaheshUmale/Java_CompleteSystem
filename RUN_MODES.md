# JULES-HF-ATS: Running in Live vs. Simulation Mode

This document provides instructions on how to run the JULES-HF-ATS application in its two primary modes: **Simulation (Backtest)** and **Live**.

---

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

## 1. Simulation / Backtest Mode

This mode allows you to replay historical or generated data for testing and analysis without connecting to a live data feed. The frontend for this mode is the JULES-HF-ATS dashboard you just built.

### Step 1: Configure for Simulation

In your `ats-core/src/main/resources/config.properties` file, set the `run.mode` property:

```properties
run.mode=simulation
```

### Step 2: Ensure Sample Data Exists

The simulation mode relies on a sample data file. You can generate one using the provided script:

```bash
python3 scripts/generate_data.py
```

This will create the necessary `generated_data.json.gz` file in the `ats-core/src/main/resources/data/` directory.

### Step 3: Run the Backend

1.  **Build the entire project** from the root directory:
    ```bash
    mvn clean install
    ```
2.  **Run the application JAR**. The backend will start, process the entire simulation data file, and keep the WebSocket server running to serve the results to the UI.
    ```bash
    java -jar ats-dashboard/target/ats-dashboard-1.0-SNAPSHOT-jar-with-dependencies.jar
    ```

### Step 4: Run the Frontend

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

## 2. Live Mode

This mode connects to a live data feed (e.g., Upstox) for real-time trading and monitoring.

### Step 1: Configure for Live Mode

1.  In your `ats-core/src/main/resources/config.properties` file, set the `run.mode` property:
    ```properties
    run.mode=live
    ```
2.  **Add your live API credentials** to the same `config.properties` file. For example, you would need to provide your Upstox access token.
    ```properties
    # Example for Upstox
    upstox.accessToken=YOUR_ACCESS_TOKEN_HERE
    ```

### Step 2: Run the Backend

1.  **Build the project**:
    ```bash
    mvn clean install
    ```
2.  **Run the application JAR**. The backend will initialize the connection to the live data feed and start the WebSocket server.
    ```bash
    java -jar ats-dashboard/target/ats-dashboard-1.0-SNAPSHOT-jar-with-dependencies.jar
    ```

### Step 3: Run the Frontend

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
