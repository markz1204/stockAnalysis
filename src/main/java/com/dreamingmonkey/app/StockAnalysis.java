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

    public static void main(String[] args) throws IOException {

        StockAnalysis stockAnalysis = new StockAnalysis();

        Set<String> qualified = stockAnalysis.getASXCodes().stream().filter(code->isQualified(code)).collect(Collectors.toSet());

        System.out.println(qualified.size() + " stocks are qualified");
        System.out.println(qualified.toString());
    }

    private static boolean isQualified(String symbol){
        Calendar from = Calendar.getInstance();
        Calendar to = Calendar.getInstance();
        from.add(Calendar.MONTH, -1);


        try {
            Stock stock = YahooFinance.get(symbol + ".AX", from, to, Interval.DAILY);

            List<HistoricalQuote> historicalQuoteList = stock.getHistory();

            //Get recent 20 days quotes.
            historicalQuoteList = historicalQuoteList.subList(0, 20);

            OptionalDouble average20 = historicalQuoteList.stream().mapToDouble(hq->hq.getClose().doubleValue()).average();

            return historicalQuoteList.get(0).getClose().doubleValue() > average20.getAsDouble() && historicalQuoteList.get(0).getClose().doubleValue() < average20.getAsDouble();

        } catch (IOException e) {
            System.out.println("Error happened when processing " + symbol);
        }

        return false;
    }

    private List<String> getASXCodes(){

        BufferedReader br = null;
        String line = "";
        String cvsSplitBy = ",";
        List<String> codes = new ArrayList<>();

        File source = null;

        if(!Paths.get(ASX_CODES_CSV).isAbsolute()){
            ClassLoader classLoader = getClass().getClassLoader();
            source = new File(classLoader.getResource(ASX_CODES_CSV).getFile());
        }else{
            source = new File(ASX_CODES_CSV);
        }

        try {

            br = new BufferedReader(new FileReader(source));
            while ((line = br.readLine()) != null) {

                // use comma as separator
                String[] company = line.split(cvsSplitBy);

                codes.add(company[1]);
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
