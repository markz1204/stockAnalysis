package com.dreamingmonkey.app;

import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.histquotes.HistoricalQuote;
import yahoofinance.histquotes.Interval;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class Average20 {

    private String minPrice,maxPrice;

    public Average20(String minPrice, String maxPrice){
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
    }

    public boolean isQualified(String symbol, boolean isAbove20Avg) {
        Calendar from = Calendar.getInstance();
        Calendar to = Calendar.getInstance();
        from.add(Calendar.MONTH, -2);


        try {
            Stock stock = YahooFinance.get(symbol + ".AX", from, to, Interval.DAILY);

            if(stock == null)
                return false;

            List<HistoricalQuote> historicalQuoteList = Collections.synchronizedList(stock.getHistory());

            historicalQuoteList = historicalQuoteList.parallelStream().filter(hq->(hq.getVolume() != null && hq.getVolume() > 0)).sorted(Comparator.comparing(HistoricalQuote::getDate).reversed()).collect(Collectors.toList());

            if (historicalQuoteList.size() < 21) {
                return false;
            }

            HistoricalQuote latest = historicalQuoteList.get(0);

            //Get recent 20 days quotes.
            historicalQuoteList = historicalQuoteList.subList(1, 21);

            OptionalDouble average20 = historicalQuoteList.stream().mapToDouble(hq -> hq.getAdjClose() != null ? hq.getAdjClose().doubleValue() : 0.00).average();

            if(average20.getAsDouble() >= Double.valueOf(minPrice) && average20.getAsDouble() <= Double.valueOf(maxPrice)) {
                if (isAbove20Avg) {
                    return latest.getAdjClose().doubleValue() > average20.getAsDouble() && historicalQuoteList.get(0).getAdjClose().doubleValue() < average20.getAsDouble();
                } else {
                    return latest.getAdjClose().doubleValue() < average20.getAsDouble() && historicalQuoteList.get(0).getAdjClose().doubleValue() > average20.getAsDouble();
                }
            }

        } catch (IOException e) {
            //System.out.println("File data not found when processing " + symbol);
        }

        return false;
    }

}
