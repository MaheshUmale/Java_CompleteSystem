package com.trading.hf;

import com.lmax.disruptor.EventHandler;
import org.json.JSONObject;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class IndexWeightCalculator implements EventHandler<MarketEvent> {

    private final Map<String, Heavyweight> heavyweights;
    private volatile double aggregateWeightedDelta = 0.0;

    public IndexWeightCalculator(String indexPath) {
        this.heavyweights = loadWeights(indexPath);
    }

    @Override
    public void onEvent(MarketEvent event, long sequence, boolean endOfBatch) {
        Heavyweight hw = heavyweights.get(event.getSymbol());
        if (hw != null) {
            double delta = (event.getTbq() - event.getTsq());
            hw.setDelta(delta);
            updateAggregateWeightedDelta();
        }
    }

    private void updateAggregateWeightedDelta() {
        aggregateWeightedDelta = heavyweights.values().stream()
                .mapToDouble(hw -> hw.getDelta() * hw.getWeight())
                .sum();
    }

    public double getAggregateWeightedDelta() {
        return aggregateWeightedDelta;
    }

    public Map<String, Heavyweight> getHeavyweights() {
        return heavyweights;
    }

    private Map<String, Heavyweight> loadWeights(String path) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                throw new RuntimeException("Cannot find resource: " + path);
            }
            String jsonText = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            JSONObject json = new JSONObject(jsonText);
            JSONObject nifty50 = json.getJSONObject("NIFTY50");

            Map<String, Heavyweight> tempMap = new ConcurrentHashMap<>();
            for (String key : nifty50.keySet()) {
                double weight = nifty50.getDouble(key);
                tempMap.put(key, new Heavyweight(key, weight));
            }
            return tempMap;

        } catch (Exception e) {
            throw new RuntimeException("Failed to load index weights", e);
        }
    }

    public static class Heavyweight {
        private final String name;
        private final double weight;
        private volatile double delta;

        public Heavyweight(String name, double weight) {
            this.name = name;
            this.weight = weight;
            this.delta = 0.0;
        }

        public String getName() {
            return name;
        }

        public double getWeight() {
            return weight;
        }

        public double getDelta() {
            return delta;
        }

        public void setDelta(double delta) {
            this.delta = delta;
        }
    }
}
