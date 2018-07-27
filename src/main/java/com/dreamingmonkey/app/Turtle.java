package com.dreamingmonkey.app;

import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.histquotes.HistoricalQuote;
import yahoofinance.histquotes.Interval;

import java.io.IOException;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

public class Turtle {

    private String minPrice,maxPrice;

    public Turtle(String minPrice, String maxPrice){
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
    }

    public boolean isQualified(String symbol, boolean decideToBuy) {

        Calendar from = Calendar.getInstance();
        Calendar to = Calendar.getInstance();
        from.add(Calendar.MONTH, -2);


        try {
            Stock stock = YahooFinance.get(symbol + ".AX", from, to, Interval.DAILY);

            if(stock == null)
                return false;

            List<HistoricalQuote> historicalQuoteList = stock.getHistory();

            historicalQuoteList = historicalQuoteList.parallelStream().filter(hq->(hq.getVolume() != null && hq.getVolume() > 0)).sorted(Comparator.comparing(HistoricalQuote::getDate).reversed()).collect(Collectors.toList());

            if (historicalQuoteList.size() < 21) {
                return false;
            }

            HistoricalQuote latest = historicalQuoteList.get(0);

            //Get recent 20 days quotes.
            List<HistoricalQuote> historical20QuoteList = historicalQuoteList.subList(1, 21);

            //Get recent 10 days quotes
            List<HistoricalQuote> historical10QuoteList = historicalQuoteList.subList(1, 11);

            OptionalDouble average20 = historicalQuoteList.stream().mapToDouble(hq -> hq.getAdjClose() != null ? hq.getAdjClose().doubleValue() : 0.00).average();

            OptionalDouble average20Highest = historical20QuoteList.stream().mapToDouble(hq -> hq.getHigh() != null ? hq.getHigh().doubleValue() : 0.00).max();
            OptionalDouble average10Lowest = historical10QuoteList.stream().mapToDouble(hq -> hq.getLow() != null ? hq.getLow().doubleValue() : 0.00).min();


            if(average20.getAsDouble() >= Double.valueOf(minPrice) && average20.getAsDouble() <= Double.valueOf(maxPrice)) {
                if (decideToBuy) {
                    return latest.getAdjClose().doubleValue() > average20Highest.getAsDouble() && historical20QuoteList.get(0).getAdjClose().doubleValue() <= average20Highest.getAsDouble();
                } else {
                    return latest.getAdjClose().doubleValue() < average10Lowest.getAsDouble() && historical10QuoteList.get(0).getAdjClose().doubleValue() >= average10Lowest.getAsDouble();
                }
            }

        } catch (IOException e) {
            //System.out.println("File data not found when processing " + symbol);
        }

        return false;
    }
}
