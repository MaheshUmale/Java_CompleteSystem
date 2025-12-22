package com.trading.hf;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.lmax.disruptor.RingBuffer;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

public class MySampleDataReplayer implements IDataReplayer {

    private final RingBuffer<MarketEvent> ringBuffer;
    private final String dataDirectory;
    private final Gson gson = new Gson();
    private final long simulationEventDelayMs;

    // Use a single, generated data file for predictable backtesting
    private final List<String> dataFiles = Arrays.asList(
            "NSE_EQ_INE027H01010_data.json.json.gz");

    public MySampleDataReplayer(RingBuffer<MarketEvent> ringBuffer, String dataDirectory) {
        this.ringBuffer = ringBuffer;
        this.dataDirectory = dataDirectory;
        this.simulationEventDelayMs = Long.parseLong(ConfigLoader.getProperty("simulation.event.delay.ms", "10"));
    }

    public void start() {
        System.out.println("Starting data replay from classpath directory: " + dataDirectory);
        while (true) {
            for (String fileName : dataFiles) {
                processFile(dataDirectory + "/" + fileName);
            }
            try {
                Thread.sleep(1000); // Add a 1-second delay
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Data replay interrupted");
                return;
            }
        }
    }

    private void processFile(String filePath) {
        System.out.println("Processing file: " + filePath);
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(filePath);
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(new GZIPInputStream(Objects.requireNonNull(is))))) {

            String jsonData = reader.lines().collect(Collectors.joining());
            Type type = new TypeToken<List<Map<String, Object>>>() {
            }.getType();
            List<Map<String, Object>> dataList = gson.fromJson(jsonData, type);

            for (Map<String, Object> data : dataList) {
                // Transform the record to the new structure
                Map<String, Object> restructuredData = transformRecord(data);
                publishMarketUpdate(restructuredData);
                try {
                    Thread.sleep(simulationEventDelayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.err.println("Data replay interrupted");
                    return;
                }
            }

        } catch (Exception e) {
            System.err.println("Error processing file " + filePath + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private void publishMarketUpdate(Map<String, Object> data) {
        try {
            Map<String, Object> feeds = (Map<String, Object>) data.get("feeds");
            if (feeds == null || feeds.isEmpty()) {
                return;
            }

            String instrumentKey = feeds.keySet().iterator().next();
            Map<String, Object> feedData = (Map<String, Object>) feeds.get(instrumentKey);
            Map<String, Object> ff = (Map<String, Object>) feedData.get("fullFeed");

            Map<String, Object> marketData = (Map<String, Object>) ff.get("marketFF");
            if (marketData == null) {
                marketData = (Map<String, Object>) ff.get("indexFF");
            }
            if (marketData == null)
                return;

            Map<String, Object> ltpc = (Map<String, Object>) marketData.get("ltpc");
            if (ltpc == null)
                return;

            double ltp = ((Number) ltpc.get("ltp")).doubleValue();
            long ltq = Long.parseLong((String) ltpc.get("ltq"));
            long ltt = Long.parseLong((String) ltpc.get("ltt"));
            double cp = ((Number) ltpc.get("cp")).doubleValue();

            double tbq = 0, tsq = 0, atp = 0, oi = 0;
            long vtt = 0;
            double bestBidPrice = 0, bestAskPrice = 0;

            if (marketData.containsKey("tbq"))
                tbq = ((Number) marketData.get("tbq")).doubleValue();
            if (marketData.containsKey("tsq"))
                tsq = ((Number) marketData.get("tsq")).doubleValue();
            if (marketData.containsKey("atp"))
                atp = ((Number) marketData.get("atp")).doubleValue();
            if (marketData.containsKey("vtt"))
                vtt = Long.parseLong((String) marketData.get("vtt"));
            if (marketData.containsKey("oi"))
                oi = ((Number) marketData.get("oi")).doubleValue();

            if (marketData.containsKey("marketLevel")) {
                Map<String, Object> marketLevel = (Map<String, Object>) marketData.get("marketLevel");
                List<Map<String, Object>> bidAskQuote = (List<Map<String, Object>>) marketLevel.get("bidAskQuote");
                if (bidAskQuote != null && !bidAskQuote.isEmpty()) {
                    bestBidPrice = ((Number) bidAskQuote.get(0).get("bidP")).doubleValue();
                    bestAskPrice = ((Number) bidAskQuote.get(0).get("askP")).doubleValue();
                }
            }

            long sequence = ringBuffer.next();
            try {
                MarketEvent event = ringBuffer.get(sequence);
                event.setSymbol(instrumentKey);
                event.setLtp(ltp);
                event.setLtt(ltt);
                event.setLtq(ltq);
                event.setCp(cp);
                event.setTbq(tbq);
                event.setTsq(tsq);
                event.setVtt(vtt);
                event.setOi(oi);
                event.setIv(0);
                event.setAtp(atp);
                event.setTs(ltt);
                event.setBestBidPrice(bestBidPrice);
                event.setBestAskPrice(bestAskPrice);
            } finally {
                ringBuffer.publish(sequence);
            }

        } catch (Exception e) {
            System.err.println("Error mapping data: " + e.getMessage() + " on line " + gson.toJson(data));
            e.printStackTrace();
        }
    }

    /**
     * Converts raw flat record into the nested "feeds" structure.
     */
    private Map<String, Object> transformRecord(Map<String, Object> raw) {
        String instrumentKey = (String) raw.get("instrumentKey");
        Object fullFeed = raw.get("fullFeed");

        // Inner object: contains fullFeed and requestMode
        Map<String, Object> innerData = new java.util.HashMap<>();
        innerData.put("fullFeed", fullFeed);
        innerData.put("requestMode", "full_d5");

        // Feeds map: instrumentKey -> innerData
        Map<String, Object> instrumentMap = new java.util.HashMap<>();
        instrumentMap.put(instrumentKey, innerData);

        // Root map: "feeds" -> instrumentMap
        Map<String, Object> root = new java.util.HashMap<>();
        root.put("feeds", instrumentMap);

        return root;
    }

}
