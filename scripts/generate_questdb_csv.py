import csv
import os
from datetime import datetime, timedelta
import random

def generate_questdb_csv(
    instrument_key="NSE_EQ_TEST",
    output_file="questdb_market_data.csv",
    start_time_str="2024-01-01 09:15:00",
    end_time_str="2024-01-01 15:30:00",
    ticks_per_second=2
):
    """
    Generates a CSV file with sample market data for QuestDB.
    """
    start_time = datetime.fromisoformat(start_time_str)
    end_time = datetime.fromisoformat(end_time_str)
    current_time = start_time
    time_increment = timedelta(seconds=1.0 / ticks_per_second)

    start_price = 100.0
    closing_price = 99.5
    total_buy_quantity = 50000
    total_sell_quantity = 45000
    volume_today = 1000000
    open_interest = 10000

    header = [
        "instrument_key", "ltp", "ltq", "cp", "bid_price", "ask_price",
        "total_buy_quantity", "total_sell_quantity", "volume_today",
        "avg_trade_price", "open_interest", "event_timestamp"
    ]

    with open(output_file, "w", newline="") as f:
        writer = csv.writer(f)
        writer.writerow(header)

        record_count = 0
        while current_time <= end_time:
            price_movement = random.uniform(-0.1, 0.1)
            ltp = round(start_price + price_movement, 2)
            ltq = random.randint(1, 100)
            bid_price = round(ltp - random.uniform(0.01, 0.05), 2)
            ask_price = round(ltp + random.uniform(0.01, 0.05), 2)

            total_buy_quantity += random.randint(10, 50)
            total_sell_quantity += random.randint(10, 50)
            volume_today += ltq
            avg_trade_price = round(
                (volume_today * ltp) / (volume_today + 1), 2
            )
            # QuestDB timestamp format: YYYY-MM-DDTHH:MM:SS.mmmmmmZ
            timestamp_str = current_time.strftime('%Y-%m-%dT%H:%M:%S.%f')[:-3] + 'Z'

            row = [
                instrument_key, ltp, ltq, closing_price, bid_price, ask_price,
                total_buy_quantity, total_sell_quantity, volume_today,
                avg_trade_price, open_interest, timestamp_str
            ]
            writer.writerow(row)

            current_time += time_increment
            start_price = ltp
            record_count += 1

    print(f"Generated {record_count} records in {output_file}")


if __name__ == "__main__":
    # The output is placed in the scripts directory itself for easy access.
    # QuestDB's bulk import can be pointed to this file.
    output_dir = "scripts"
    if not os.path.exists(output_dir):
        os.makedirs(output_dir)

    output_file_path = os.path.join(output_dir, "questdb_market_data.csv")
    generate_questdb_csv(output_file=output_file_path)
