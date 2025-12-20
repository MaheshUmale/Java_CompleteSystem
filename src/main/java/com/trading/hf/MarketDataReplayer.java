package com.trading.hf;

import com.lmax.disruptor.RingBuffer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;

public class MarketDataReplayer {

    private final RingBuffer<MarketEvent> ringBuffer;
    private final String questdbConnectionString;

    public MarketDataReplayer(RingBuffer<MarketEvent> ringBuffer, String questdbConnectionString) {
        this.ringBuffer = ringBuffer;
        this.questdbConnectionString = questdbConnectionString;
    }

    public void replay(String symbol, Instant startTime, Instant endTime) {
        String query = String.format(
                "SELECT * FROM ticks WHERE symbol = '%s' AND ts BETWEEN '%s' AND '%s' ORDER BY ts ASC",
                symbol, startTime, endTime
        );

        try (Connection conn = DriverManager.getConnection(questdbConnectionString);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                long sequence = ringBuffer.next();
                try {
                    MarketEvent event = ringBuffer.get(sequence);
                    event.setSymbol(rs.getString("symbol"));
                    event.setLtp(rs.getDouble("ltp"));
                    event.setLtq(rs.getLong("ltq"));
                    event.setLtt(rs.getTimestamp("ts").getTime());
                    // ... set other fields from the result set
                } finally {
                    ringBuffer.publish(sequence);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
