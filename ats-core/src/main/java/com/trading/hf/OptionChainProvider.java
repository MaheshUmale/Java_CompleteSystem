package com.trading.hf;

import com.lmax.disruptor.EventHandler;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.Comparator;

public class OptionChainProvider implements EventHandler<MarketEvent> {

    private final Map<String, MarketEvent> optionState = new ConcurrentHashMap<>();
    private final Map<String, Double> previousOi = new ConcurrentHashMap<>();
    private final AtomicReference<Double> spotPrice = new AtomicReference<>(0.0);
    private static final int STRIKE_DIFFERENCE = 50;
    private static final int WINDOW_SIZE = 2; // ATM +/- 2 strikes

    @Override
    public void onEvent(MarketEvent event, long sequence, boolean endOfBatch) {
        String symbol = event.getSymbol();
        if (symbol == null) return;

        if ("NSE_INDEX|Nifty 50".equals(symbol)) {
            spotPrice.set(event.getLtp());
        } else if (symbol.contains(" CE") || symbol.contains(" PE")) {
            optionState.put(symbol, event);
        }
    }

    public List<OptionChainDto> getOptionChainWindow() {
        double currentSpot = spotPrice.get();
        if (currentSpot == 0.0) {
            return List.of();
        }

        int atmStrike = (int) (Math.round(currentSpot / STRIKE_DIFFERENCE) * STRIKE_DIFFERENCE);

        return optionState.values().stream()
                .map(event -> {
                    SymbolUtil.OptionSymbol optionSymbol = SymbolUtil.parseOptionSymbol(event.getSymbol());
                    if (optionSymbol == null) return null;

                    int strike = optionSymbol.getStrike();
                    int lowerBound = atmStrike - (WINDOW_SIZE * STRIKE_DIFFERENCE);
                    int upperBound = atmStrike + (WINDOW_SIZE * STRIKE_DIFFERENCE);

                    if (strike >= lowerBound && strike <= upperBound) {
                        double currentOi = event.getOi();
                        double prevOi = previousOi.getOrDefault(event.getSymbol(), currentOi);
                        double oiChangePercent = (prevOi == 0) ? 0 : ((currentOi - prevOi) / prevOi) * 100;
                        previousOi.put(event.getSymbol(), currentOi);

                        return new OptionChainDto(
                                strike,
                                optionSymbol.getType(),
                                event.getLtp(),
                                oiChangePercent,
                                "NEUTRAL"
                        );
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(OptionChainDto::getStrike))
                .collect(Collectors.toList());
    }
}
