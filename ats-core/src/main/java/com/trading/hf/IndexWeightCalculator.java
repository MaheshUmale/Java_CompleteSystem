package com.trading.hf;

import com.lmax.disruptor.EventHandler;
import org.json.JSONObject;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class IndexWeightCalculator implements EventHandler<MarketEvent> {

    private final Map<String, Double> indexWeights;
    private final ConcurrentHashMap<String, Double> instrumentDeltas = new ConcurrentHashMap<>();
    private volatile double weightedIndexDelta = 0.0;

    public IndexWeightCalculator(String indexPath) {
        this.indexWeights = loadWeights(indexPath);
    }

    @Override
    public void onEvent(MarketEvent event, long sequence, boolean endOfBatch) {
        if (indexWeights.containsKey(event.getSymbol())) {
            double delta = (event.getTbq() - event.getTsq());
            instrumentDeltas.put(event.getSymbol(), delta);
            updateWeightedIndexDelta();
        }
    }

    private void updateWeightedIndexDelta() {
        weightedIndexDelta = instrumentDeltas.entrySet().stream()
                .mapToDouble(entry -> entry.getValue() * indexWeights.getOrDefault(entry.getKey(), 0.0))
                .sum();
    }

    public double getWeightedIndexDelta() {
        return weightedIndexDelta;
    }

    private Map<String, Double> loadWeights(String path) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                throw new RuntimeException("Cannot find resource: " + path);
            }
            String jsonText = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            JSONObject json = new JSONObject(jsonText);
            JSONObject nifty50 = json.getJSONObject("NIFTY50");
            return nifty50.keySet().stream()
                    .collect(Collectors.toMap(key -> key, key -> nifty50.getDouble(key)));
        } catch (Exception e) {
            throw new RuntimeException("Failed to load index weights", e);
        }
    }
}
