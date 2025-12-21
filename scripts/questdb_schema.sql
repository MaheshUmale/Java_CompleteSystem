CREATE TABLE market_data (
    instrument_key SYMBOL INDEX,
    ltp DOUBLE,
    ltq LONG,
    cp DOUBLE,
    bid_price DOUBLE,
    ask_price DOUBLE,
    total_buy_quantity LONG,
    total_sell_quantity LONG,
    volume_today LONG,
    avg_trade_price DOUBLE,
    open_interest LONG,
    event_timestamp TIMESTAMP
) timestamp(event_timestamp) PARTITION BY DAY;
