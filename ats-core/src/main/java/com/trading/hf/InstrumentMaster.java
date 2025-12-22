package com.trading.hf;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Comparator;
import java.util.Optional;

public class InstrumentMaster {

    private final Map<Integer, Map<String, InstrumentDefinition>> instrumentMap = new ConcurrentHashMap<>();
    private final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public InstrumentMaster(String resourcePath) {
        loadInstruments(resourcePath);
    }

    private void loadInstruments(String resourcePath) {
        Gson gson = new Gson();
        Type listType = new TypeToken<List<InstrumentDefinition>>() {}.getType();

        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new RuntimeException("Cannot find resource: " + resourcePath);
            }
            List<InstrumentDefinition> instruments = gson.fromJson(new InputStreamReader(is), listType);
            for (InstrumentDefinition instrument : instruments) {
                instrumentMap
                        .computeIfAbsent(instrument.getStrikePrice(), k -> new ConcurrentHashMap<>())
                        .put(instrument.getOptionType(), instrument);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load instrument master", e);
        }
    }

    public Optional<String> findInstrumentKey(int strike, String optionType) {
        // For simplicity, we'll find the first available instrument for the nearest expiry.
        // A real implementation would need more sophisticated expiry management.
        return Optional.ofNullable(instrumentMap.get(strike))
                .map(expiryMap -> expiryMap.get(optionType))
                .map(InstrumentDefinition::getInstrumentKey);
    }

    public static class InstrumentDefinition {
        private String instrument_key;
        private int strike_price;
        private String option_type;
        private String expiry;

        public String getInstrumentKey() {
            return instrument_key;
        }

        public int getStrikePrice() {
            return strike_price;
        }

        public String getOptionType() {
            return option_type;
        }

        public LocalDate getExpiry() {
            return LocalDate.parse(expiry);
        }
    }
}
