# Database Documentation

This document provides details on the database storage for the High-Frequency Auction Trading System (HF-ATS). The system uses QuestDB for persisting market data.

## Data Persistence Feature

The persistence of raw market data from the Upstox WebSocket feed can be enabled or disabled via a feature flag in the `config.properties` file.

- **To enable persistence:**
  Set `database.persistence.enabled=true` in `config.properties`.

- **To disable persistence:**
  Set `database.persistence.enabled=false` in `config.properties`.

When disabled, no data from the live feed will be written to the database, reducing system overhead.

## Table Schema: `raw_market_feed`

This table stores the raw, unfiltered market data feed for each instrument. This data is essential for high-fidelity backtesting and trade replay analysis.

| Column Name     | Data Type  | Description                                                                                                 |
|-----------------|------------|-------------------------------------------------------------------------------------------------------------|
| `timestamp`     | `TIMESTAMP`| The timestamp of the event, designated as the QuestDB `designated timestamp`.                               |
| `instrumentKey` | `SYMBOL`   | The unique identifier for the financial instrument (e.g., `NSE_EQ|INE020B01026`). Indexed for fast lookups.  |
| `ltp`           | `DOUBLE`   | The Last Traded Price for the instrument at the time of the event.                                          |
| `ltq`           | `LONG`     | The Last Traded Quantity for the instrument at the time of the event.                                       |
| `bids`          | `STRING`   | A JSON array representing the full bid side of the order book. Each entry includes price, quantity, and orders. |
| `asks`          | `STRING`   | A JSON array representing the full ask side of the order book. Each entry includes price, quantity, and orders. |

### Order Book JSON Structure

The `bids` and `asks` columns contain a JSON string representing a list of order book entries. Each entry in the list is an object with the following structure:

```json
[
  {
    "price": 150.25,
    "quantity": 100,
    "orders": 5
  },
  {
    "price": 150.20,
    "quantity": 250,
    "orders": 12
  }
]
```

## Data Management

The `raw_market_feed` table can grow very large, especially when tracking many instruments in a live environment. It is recommended to have a data retention or archival policy in place. QuestDB provides partitioning and other features to manage large time-series datasets effectively.
