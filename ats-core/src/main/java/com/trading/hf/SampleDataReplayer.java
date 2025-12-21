package com.trading.hf;

import com.google.protobuf.util.JsonFormat;
import com.lmax.disruptor.RingBuffer;
import com.trading.hf.logic.IDataReplayer;
import com.upstox.marketdatafeeder.rpc.proto.FeedResponse;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

public class SampleDataReplayer implements IDataReplayer {
    private final RingBuffer<MarketEvent> ringBuffer;
    private final String dataDirectory;

    public SampleDataReplayer(RingBuffer<MarketEvent> ringBuffer, String dataDirectory) {
        this.ringBuffer = ringBuffer;
        this.dataDirectory = dataDirectory;
    }

    @Override
    public void start() {
        try {
            String filePath = dataDirectory + "/generated_data.json.gz";
            GZIPInputStream gzip = new GZIPInputStream(ConfigLoader.class.getClassLoader().getResourceAsStream(filePath));
            BufferedReader br = new BufferedReader(new InputStreamReader(gzip));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            JSONArray jsonArray = new JSONArray(sb.toString());

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject tick = jsonArray.getJSONObject(i);

                long sequence = ringBuffer.next();
                try {
                    MarketEvent event = ringBuffer.get(sequence);
                    FeedResponse.Builder builder = FeedResponse.newBuilder();
                    JsonFormat.parser().ignoringUnknownFields().merge(tick.toString(), builder);
                    event.setFeedResponse(builder.build());
                } finally {
                    ringBuffer.publish(sequence);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
