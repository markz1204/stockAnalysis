package com.dreamingmonkey.app;

import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.histquotes.HistoricalQuote;
import yahoofinance.histquotes.Interval;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by MZhang on 12/05/2017.
 */
public class StockAnalysis {

    public static final String ASX_CODES_CSV = System.getProperty("asx_codes", "companies.csv");
    public static final String SHOW_BOTH = System.getProperty("both", "false");
    public static final String MINI_PRICE = System.getProperty("mini", "1");


    public static void main(String[] args) throws IOException {

        StockAnalysis stockAnalysis = new StockAnalysis();

        Set<String> below20Avg = null;

        long start = System.currentTimeMillis();

        Set<String> above20Avg = stockAnalysis.getASXCodes().stream().parallel().filter(code -> isQualified(code, true)).collect(Collectors.toSet());

        if (Boolean.TRUE.equals(Boolean.valueOf(SHOW_BOTH))) {
            below20Avg = stockAnalysis.getASXCodes().stream().parallel().filter(code -> isQualified(code, false)).collect(Collectors.toSet());
        }

        long end = System.currentTimeMillis();

        long total = end - start;
        System.out.println("Takes: " + total / 1000 + "s");

        System.out.println(above20Avg.size() + " stocks are above 20 average");
        System.out.println(above20Avg.toString());

        if (Boolean.TRUE.equals(Boolean.valueOf(SHOW_BOTH))) {
            System.out.println(below20Avg.size() + " stocks are below 20 average");
            System.out.println(below20Avg.toString());
        }
    }

    private static boolean isQualified(String symbol, boolean isAbove20Avg) {
        Calendar from = Calendar.getInstance();
        Calendar to = Calendar.getInstance();
        from.add(Calendar.MONTH, -2);


        try {
            Stock stock = YahooFinance.get(symbol + ".AX", from, to, Interval.DAILY);

            List<HistoricalQuote> historicalQuoteList = stock.getHistory();

            if (historicalQuoteList.size() < 20) {
                return false;
            }

            //Get recent 20 days quotes.
            historicalQuoteList = historicalQuoteList.subList(0, 20);

            OptionalDouble average20 = historicalQuoteList.stream().mapToDouble(hq -> hq.getClose().doubleValue()).average();

            if(average20.getAsDouble() > Double.valueOf(MINI_PRICE)) {
                if (isAbove20Avg) {
                    return historicalQuoteList.get(0).getClose().doubleValue() > average20.getAsDouble() && historicalQuoteList.get(1).getClose().doubleValue() < average20.getAsDouble();
                } else {
                    return historicalQuoteList.get(0).getClose().doubleValue() < average20.getAsDouble() && historicalQuoteList.get(1).getClose().doubleValue() > average20.getAsDouble();
                }
            }

        } catch (IOException e) {
            System.out.println("Error happened when processing " + symbol);
        }

        return false;
    }

    private List<String> getASXCodes() {

        BufferedReader br = null;
        String line = "";
        String cvsSplitBy = ",";
        List<String> codes = new ArrayList<>();

        File source = null;

        if (!Paths.get(ASX_CODES_CSV).isAbsolute()) {
            ClassLoader classLoader = getClass().getClassLoader();
            source = new File(classLoader.getResource(ASX_CODES_CSV).getFile());
        } else {
            source = new File(ASX_CODES_CSV);
        }

        try {

            br = new BufferedReader(new FileReader(source));
            while ((line = br.readLine()) != null) {

                // use comma as separator
                String[] company = line.split(cvsSplitBy);

                if (company.length > 1) {
                    codes.add(company[1]);
                }
            }

        } catch (FileNotFoundException e) {
            System.out.println("No source companies file found...");
        } catch (IOException e) {
            System.out.println("Error happened while reading source companies file..");
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return codes;
    }

}
