package com.dreamingmonkey.app;

import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by MZhang on 12/05/2017.
 */
public class StockAnalysis {

    private static final String ASX_CODES_CSV = System.getProperty("asx_codes", "companies.csv");
    private static String SHOW_2_SIDES = System.getProperty("2sides", "");
    private static String STRATEGIES= System.getProperty("strategies", "");
    private static String MIN_PRICE = System.getProperty("min", "");
    private static String MAX_PRICE = System.getProperty("max", "");
    private static String SINGLE_CHECK = System.getProperty("single", "");
    private List<String> codes = null;


    public static void main(String[] args) throws IOException, InterruptedException {

        if(!SINGLE_CHECK.isEmpty()){
            Turtle turtle = new Turtle(MIN_PRICE, MAX_PRICE);

            System.out.println("Sell now ? " + turtle.isQualified(SINGLE_CHECK, false));
        }else{
            inputConfig();

            System.out.println("Starting process, Price range " + MIN_PRICE + " TO " + MAX_PRICE + ", Strategies: " + STRATEGIES);

            StockAnalysis stockAnalysis = new StockAnalysis();

            ExecutorService executorService = Executors.newFixedThreadPool(2);

            List<String> strategies = Arrays.asList(STRATEGIES.split(","));

            if(strategies.contains("avg20")) {
                executorService.submit(() -> {
                    try {
                        stockAnalysis.average20Result();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }

            if(strategies.contains("turtle")) {
                executorService.submit(() -> {
                    try {
                        stockAnalysis.turtleResult();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }

            executorService.awaitTermination(30, TimeUnit.MINUTES);

            if(!executorService.isShutdown()){
                executorService.shutdownNow();
            }
        }
    }

    private void average20Result() throws IOException {
        Average20 average20 = new Average20(MIN_PRICE, MAX_PRICE);

        List<String> below20Avg = null;

        long start = System.currentTimeMillis();

        List<String> above20Avg = this.getASXCodes(true).parallelStream().filter(code -> average20.isQualified(code, true)).collect(Collectors.toList());

        if (Boolean.TRUE.equals(Boolean.valueOf(SHOW_2_SIDES))) {
            below20Avg = this.getASXCodes(true).parallelStream().filter(code -> average20.isQualified(code, false)).collect(Collectors.toList());
        }

        long end = System.currentTimeMillis();

        long total = end - start;
        System.out.println("Average20 result takes: " + total / 1000 + "s");

        System.out.println(above20Avg.size() + " stocks are above 20 average");
        System.out.println(above20Avg.toString());

        if (Boolean.TRUE.equals(Boolean.valueOf(SHOW_2_SIDES))) {
            System.out.println(below20Avg.size() + " stocks are below 20 average");
            System.out.println(below20Avg.toString());
        }
    }

    private void turtleResult() throws IOException {

        Turtle turtle = new Turtle(MIN_PRICE, MAX_PRICE);

        List<String> toBuy, toSell = null;

        long start = System.currentTimeMillis();

        toBuy = this.getASXCodes(true).parallelStream().filter(code -> turtle.isQualified(code, true)).collect(Collectors.toList());

        if (Boolean.TRUE.equals(Boolean.valueOf(SHOW_2_SIDES))) {
            toSell = this.getASXCodes(true).parallelStream().filter(code -> turtle.isQualified(code, false)).collect(Collectors.toList());
        }

        long end = System.currentTimeMillis();

        long total = end - start;
        System.out.println("Turtle result takes: " + total / 1000 + "s");

        System.out.println(toBuy.size() + " stocks exceed 20 days highest");
        System.out.println(toBuy.toString());

        if (Boolean.TRUE.equals(Boolean.valueOf(SHOW_2_SIDES))) {
            System.out.println(toSell.size() + " stocks deceed 10 days lowest");
            System.out.println(toSell.toString());
        }
    }


    private List<String> getASXCodes(boolean sync) throws IOException {

        BufferedReader br = null;
        String line = "";
        String frontMark = "\",", endMark=",\"";

        if(codes != null && !codes.isEmpty())
            return codes;

        codes = new ArrayList<>();

        InputStreamReader inputStreamReader = null;

        if (!Paths.get(ASX_CODES_CSV).isAbsolute()) {
            inputStreamReader = new InputStreamReader(getClass().getResourceAsStream("/" + ASX_CODES_CSV));
        } else {
            inputStreamReader = new FileReader(new File(ASX_CODES_CSV));
        }

        try {

            br = new BufferedReader(inputStreamReader);

            while ((line = br.readLine()) != null) {

                // use comma as separator
                String company = line.substring(line.lastIndexOf(frontMark) + frontMark.length(), line.lastIndexOf(endMark));

                if (!company.isEmpty()) {
                    codes.add(company);
                }
            }

        } catch (FileNotFoundException e) {
            System.out.println("No source companies file found...");
        } catch (IOException e) {
            System.out.println("Error happened while reading source companies file..");
        }

        if(sync)
            codes = Collections.synchronizedList(codes);

        return codes;
    }

    private static void inputConfig(){
        BufferedReader br = null;

        try {

            br = new BufferedReader(new InputStreamReader(System.in));

            System.out.print("Set price range (1,100): ");
            String input = br.readLine();

            while (!("q".equals(input) || input.isEmpty())) {

                if(MIN_PRICE.isEmpty() || MAX_PRICE.isEmpty()) {
                    String[] prices = input.split(",");
                    MIN_PRICE = prices[0];
                    MAX_PRICE = prices[1];
                }


                if(STRATEGIES.isEmpty()){
                    System.out.print("Set strategies to run: (avg20, turtle): ");
                    input = br.readLine();
                    STRATEGIES = input;
                }

                if(SHOW_2_SIDES.isEmpty()){
                    System.out.print("Show one strategy's full result? ");
                    input = br.readLine();
                    SHOW_2_SIDES = input;
                }

                System.out.println("Type enter to start or q to exit.");
                input = br.readLine();
            }

            if ("q".equals(input)) {
                System.out.println("Exit!");
                System.exit(0);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
