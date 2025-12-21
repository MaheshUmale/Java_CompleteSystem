package com.trading.hf.data;

import com.lmax.disruptor.RingBuffer;
import com.trading.hf.MarketEvent;
import com.trading.hf.logic.IDataReplayer;
import com.upstox.marketdatafeeder.rpc.proto.Feed;
import com.upstox.marketdatafeeder.rpc.proto.FeedResponse;
import com.upstox.marketdatafeeder.rpc.proto.FullFeed;
import com.upstox.marketdatafeeder.rpc.proto.LTPC;
import com.upstox.marketdatafeeder.rpc.proto.MarketFullFeed;
import com.upstox.marketdatafeeder.rpc.proto.MarketLevel;
import com.upstox.marketdatafeeder.rpc.proto.Quote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.Properties;

public class QuestDbDataReplayer implements IDataReplayer {

    private static final Logger logger = LoggerFactory.getLogger(QuestDbDataReplayer.class);

    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final String jdbcUrl;
    private final RingBuffer<MarketEvent> ringBuffer;

    public QuestDbDataReplayer(Properties config, RingBuffer<MarketEvent> ringBuffer) {
        this.host = config.getProperty("questdb.host");
        this.port = Integer.parseInt(config.getProperty("questdb.port"));
        this.username = config.getProperty("questdb.username");
        this.password = config.getProperty("questdb.password");
        this.jdbcUrl = String.format("jdbc:postgresql://%s:%d/qdb?sslmode=disable", host, port);
        this.ringBuffer = ringBuffer;
    }

    @Override
    public void start() {
        logger.info("Starting data replay from QuestDB");
        try (
            Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery("SELECT * FROM market_data ORDER BY event_timestamp")
        ) {
            while (rs.next()) {
                String instrumentKey = rs.getString("instrument_key");

                LTPC ltpc = LTPC.newBuilder()
                        .setLtp(rs.getDouble("ltp"))
                        .setLtq(rs.getLong("ltq"))
                        .setCp(rs.getDouble("cp"))
                        .setLtt(rs.getTimestamp("event_timestamp").getTime())
                        .build();

                Quote quote = Quote.newBuilder()
                        .setBidP(rs.getDouble("bid_price"))
                        .setAskP(rs.getDouble("ask_price"))
                        .build();

                MarketLevel marketLevel = MarketLevel.newBuilder()
                        .addBidAskQuote(quote)
                        .build();

                MarketFullFeed marketFullFeed = MarketFullFeed.newBuilder()
                        .setLtpc(ltpc)
                        .setMarketLevel(marketLevel)
                        .setTbq(rs.getLong("total_buy_quantity"))
                        .setTsq(rs.getLong("total_sell_quantity"))
                        .setVtt(rs.getLong("volume_today"))
                        .setAtp(rs.getDouble("avg_trade_price"))
                        .setOi(rs.getLong("open_interest"))
                        .build();

                FullFeed fullFeed = FullFeed.newBuilder()
                        .setMarketFF(marketFullFeed)
                        .build();

                Feed feed = Feed.newBuilder()
                        .setFullFeed(fullFeed)
                        .build();

                FeedResponse feedResponse = FeedResponse.newBuilder()
                        .putFeeds(instrumentKey, feed)
                        .build();

                long sequence = ringBuffer.next();
                try {
                    MarketEvent event = ringBuffer.get(sequence);
                    event.setFeedResponse(feedResponse);
                } finally {
                    ringBuffer.publish(sequence);
                }
            }
        } catch (SQLException e) {
            logger.error("Error replaying data from QuestDB", e);
        }
        logger.info("Finished data replay from QuestDB");
    }
}
