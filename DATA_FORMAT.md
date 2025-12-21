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
