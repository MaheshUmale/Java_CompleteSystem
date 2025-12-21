package com.trading.hf;

import com.lmax.disruptor.EventHandler;
import com.upstox.marketdatafeeder.rpc.proto.FeedResponse;
import io.questdb.client.Sender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

public class QuestDBWriter implements AutoCloseable, EventHandler<MarketEvent> {

    private static final Logger logger = LoggerFactory.getLogger(QuestDBWriter.class);
    private final Sender sender;
    private final boolean enabled;

    public QuestDBWriter() {
        this.enabled = Boolean.parseBoolean(ConfigLoader.getProperty("questdb.enabled", "false"));
        if (enabled) {
            String host = ConfigLoader.getProperty("questdb.host", "localhost");
            int port = Integer.parseInt(ConfigLoader.getProperty("questdb.port", "9009"));
            sender = Sender.builder().address(String.format("%s:%d", host, port)).build();
        } else {
            sender = null;
        }
    }

    @Override
    public void onEvent(MarketEvent event, long sequence, boolean endOfBatch) {
        if (!enabled) {
            return;
        }
        FeedResponse feedResponse = event.getFeedResponse();
        feedResponse.getFeedsMap().forEach((key, feed) -> {
            sender.table("market_data")
                    .symbol("instrument_key", key)
                    .doubleColumn("ltp", feed.getFullFeed().getMarketFF().getLtpc().getLtp())
                    .longColumn("ltq", feed.getFullFeed().getMarketFF().getLtpc().getLtq())
                    .doubleColumn("cp", feed.getFullFeed().getMarketFF().getLtpc().getCp())
                    .doubleColumn("bid_price", feed.getFullFeed().getMarketFF().getMarketLevel().getBidAskQuote(0).getBidP())
                    .doubleColumn("ask_price", feed.getFullFeed().getMarketFF().getMarketLevel().getBidAskQuote(0).getAskP())
                    .longColumn("total_buy_quantity", (long) feed.getFullFeed().getMarketFF().getTbq())
                    .longColumn("total_sell_quantity", (long) feed.getFullFeed().getMarketFF().getTsq())
                    .longColumn("volume_today", feed.getFullFeed().getMarketFF().getVtt())
                    .doubleColumn("avg_trade_price", feed.getFullFeed().getMarketFF().getAtp())
                    .longColumn("open_interest", (long) feed.getFullFeed().getMarketFF().getOi())
                    .at(Instant.ofEpochMilli(feed.getFullFeed().getMarketFF().getLtpc().getLtt()));
        });
        if (endOfBatch) {
            sender.flush();
        }
    }

    @Override
    public void close() {
        if (sender != null) {
            sender.close();
        }
    }
}
