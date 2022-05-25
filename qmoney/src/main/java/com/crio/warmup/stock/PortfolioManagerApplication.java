      
package com.crio.warmup.stock;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import com.crio.warmup.stock.log.UncaughtExceptionHandler;
import com.crio.warmup.stock.portfolio.PortfolioManager;
import com.crio.warmup.stock.portfolio.PortfolioManagerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.nio.file.Files;
import javax.swing.border.EtchedBorder;
import com.crio.warmup.stock.dto.AnnualizedReturn;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.crio.warmup.stock.dto.TotalReturnsDto;
import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestTemplate;



public class PortfolioManagerApplication {

  static String getToken(){
    return "3abc35730872945cd85be0ff958219a363453d23";
  }
  // TODO: CRIO_TASK_MODULE_CALCULATIONS
  //  Now that you have the list of PortfolioTrade and their data, calculate annualized returns
  //  for the stocks provided in the Json.
  //  Use the function you just wrote #calculateAnnualizedReturns.
  //  Return the list of AnnualizedReturns sorted by annualizedReturns in descending order.

  // Note:
  // 1. You may need to copy relevant code from #mainReadQuotes to parse the Json.
  // 2. Remember to get the latest quotes from Tiingo API.


  // TODO:
  //  Ensure all tests are passing using below command
  //  ./gradlew test --tests ModuleThreeRefactorTest
  static Double getOpeningPriceOnStartDate(List<Candle> candles) {
    Double open = 0.0;
     for(Candle candle : candles){
       if(candle.getOpen()!=null){
         open = candle.getOpen();
         break;
       }
     }
     return open;
  }


  public static Double getClosingPriceOnEndDate(List<Candle> candles) {
    Double close = 0.0;

    // candles.sort(Comparator.comparing((o1,o2) -> o1.getDate().));
    for(int i = candles.size()-1; i >=0; i--){
      if(candles.get(i).getClose() != null){
        close = candles.get(i).getClose();
        break;
      }
    }
    return close;
  }


  public static List<Candle> fetchCandles(PortfolioTrade trade, LocalDate endDate, String token) {
     String url = prepareUrl(trade, endDate, token);
     List<Candle> candles = getCandles(url);
     return candles;
  }

  public static List<AnnualizedReturn> mainCalculateSingleReturn(String[] args)
      throws IOException, URISyntaxException {
        LocalDate endDate = LocalDate.parse(args[1]);
        List<PortfolioTrade> portfolioTrades = readTradesFromJson(args[0]);

        // get buy and sell price
        List<AnnualizedReturn> annualizedReturns = new ArrayList<>();
        
        for(PortfolioTrade portfolioTrade : portfolioTrades){
          List<Candle> candles = fetchCandles(portfolioTrade, endDate, getToken());
          Double buyPrice = getOpeningPriceOnStartDate(candles);
          Double sellPrice = getClosingPriceOnEndDate(candles);
          annualizedReturns.add(calculateAnnualizedReturns(endDate, portfolioTrade, buyPrice, sellPrice));
        }
    //  return annualizedReturns;
    return annualizedReturns
            .stream()
            .sorted((a,b)->b.getAnnualizedReturn().compareTo(a.getAnnualizedReturn()))
            .collect(Collectors.toList());
  }


  //  Return the populated list of AnnualizedReturn for all stocks.
  //  Annualized returns should be calculated in two steps:
  //   1. Calculate totalReturn = (sell_value - buy_value) / buy_value.
  //      1.1 Store the same as totalReturns
  //   2. Calculate extrapolated annualized returns by scaling the same in years span.
  //      The formula is:
  //      annualized_returns = (1 + total_returns) ^ (1 / total_num_years) - 1
  //      2.1 Store the same as annualized_returns

  public static AnnualizedReturn calculateAnnualizedReturns(LocalDate endDate,
      PortfolioTrade trade, Double buyPrice, Double sellPrice) {
        Double totalReturn = (sellPrice - buyPrice) / (buyPrice * 1.0);

        Double year = trade.getPurchaseDate().until(endDate, ChronoUnit.DAYS)/365.24;
        Double annualizedReturns = Math.pow((1 + totalReturn), ((1.0) / (year*1.0))) - 1;

      return new AnnualizedReturn(trade.getSymbol(), annualizedReturns, totalReturn);
  }



  //Done 

  private static File resolveFileFromResources(String filename) throws URISyntaxException {
    return Paths.get(Thread.currentThread().getContextClassLoader().getResource(filename).toURI()).toFile();
  }

  private static ObjectMapper getObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    return objectMapper;
  }

  public static List<String> mainReadFile(String[] args) throws IOException, URISyntaxException {
    File file = resolveFileFromResources(args[0]);
    PortfolioTrade[] portfolioTrades = getObjectMapper().readValue(file, PortfolioTrade[].class);
    List<String> listOfSymbols = new LinkedList<>();
    for (PortfolioTrade portfolioTrade : portfolioTrades) {
      listOfSymbols.add(portfolioTrade.getSymbol());
    }
    return listOfSymbols;
  }
  public static List<Candle> getCandles(String url){
    RestTemplate restTemplate = new RestTemplate();
    Candle[] candles = restTemplate.getForObject(url, TiingoCandle[].class);
    
    if(candles.length == 0) throw new RuntimeException();
    
    return Arrays.asList(candles);
  }

  public static List<TotalReturnsDto> totalReturns(List<PortfolioTrade> portfolioTrades, LocalDate endDate){

    List<TotalReturnsDto> totalReturnsDtos = new ArrayList<>();
    
    for (PortfolioTrade portfolioTrade : portfolioTrades) {
      String url = prepareUrl(portfolioTrade, endDate, getToken());
      url += "&sort=-date";
      
      List<Candle> candles = getCandles(url);

      for(Candle candle : candles){
        if(candle.getClose() != null){
          totalReturnsDtos.add(new TotalReturnsDto(portfolioTrade.getSymbol(), candle.getClose()));
          break;
        }
      }
    }
    // now sort the Trades according to trade close
    Collections.sort(totalReturnsDtos, new Comparator<TotalReturnsDto>(){
      @Override
      public int compare(TotalReturnsDto t1, TotalReturnsDto t2){
        if(t1.getClosingPrice() > t2.getClosingPrice()){
          return 1;
        } else if(t1.getClosingPrice() < t2. getClosingPrice()){
          return -1;
        } else {
          return 0;
        }
      }
    });

    return totalReturnsDtos;
  }

  public static List<String> mainReadQuotes(String[] args) throws IOException, URISyntaxException {
    // Parse the string to LocalDate
    LocalDate endDate = LocalDate.parse(args[1]);
    List<PortfolioTrade> portfolioTrades = readTradesFromJson(args[0]);

    // get returns for the specified closing date.
    // for each portfolio trade get the closing price and sort according to that
    List<TotalReturnsDto> totalReturnsDtos = totalReturns(portfolioTrades, endDate);
    
    List<String> symbols = new ArrayList<>();
    for(TotalReturnsDto totalReturnsDto : totalReturnsDtos){
      symbols.add(totalReturnsDto.getSymbol());
    }
    return symbols;
  }

  public static List<PortfolioTrade> readTradesFromJson(String filename) throws IOException, URISyntaxException {
    File file = resolveFileFromResources(filename);
    ObjectMapper objectMapper = getObjectMapper();
    PortfolioTrade[] portfolioTrades = objectMapper.readValue(file, PortfolioTrade[].class);
    return Arrays.asList(portfolioTrades);
  }

  // Prepare a Url for the given symbol, start-date (prachase date), end-data, and
  // token and Request from tiingo api
  public static String prepareUrl(PortfolioTrade trade, LocalDate endDate, String token) {
    return String.format("https://api.tiingo.com/tiingo/daily/%s/prices?startDate=%s&endDate=%s&token=%s",
        trade.getSymbol(), trade.getPurchaseDate(), endDate, token);
  }

  // previous
  private static void printJsonObject(Object object) throws IOException {
    Logger logger = Logger.getLogger(PortfolioManagerApplication.class.getCanonicalName());
    ObjectMapper mapper = new ObjectMapper();
    logger.info(mapper.writeValueAsString(object));
  }

  public static List<String> debugOutputs() {

    String valueOfArgument0 = "trades.json";
    String resultOfResolveFilePathArgs0 = "trades.json";
    String toStringOfObjectMapper = "ObjectMapper";
    String functionNameFromTestFileInStackTrace = "mainReadFile";
    String lineNumberFromTestFileInStackTrace = "";

    return Arrays.asList(new String[] { valueOfArgument0, resultOfResolveFilePathArgs0, toStringOfObjectMapper,
        functionNameFromTestFileInStackTrace, lineNumberFromTestFileInStackTrace });
  }

  // read content from file as a string
  // private static String readFileAsString(String fileName)throws URISyntaxException,IOException  {
  //   return new String(Files.readAllBytes(resolveFileFromResources(fileName).toPath()));
  // }

    // TODO: CRIO_TASK_MODULE_REFACTOR
  //  Once you are done with the implementation inside PortfolioManagerImpl and
  //  PortfolioManagerFactory, create PortfolioManager using PortfolioManagerFactory.
  //  Refer to the code from previous modules to get the List<PortfolioTrades> and endDate, and
  //  call the newly implemented method in PortfolioManager to calculate the annualized returns.

  // Note:
  // Remember to confirm that you are getting same results for annualized returns as in Module 3.

  public static List<AnnualizedReturn> mainCalculateReturnsAfterRefactor(String[] args)
      throws Exception {
       String filename = args[0];
       LocalDate endDate = LocalDate.parse(args[1]);
       File file = resolveFileFromResources(filename);
      //  String contents = readFileAsString(file);
       ObjectMapper objectMapper = getObjectMapper();
       RestTemplate restTemplate = new RestTemplate();
       PortfolioTrade[] portfolioTrades = objectMapper.readValue(file, PortfolioTrade[].class);
       PortfolioManager portfolioManager = PortfolioManagerFactory.getPortfolioManager("tiingo", restTemplate);
       return portfolioManager.calculateAnnualizedReturn(Arrays.asList(portfolioTrades), endDate);
  }



  public static void main(String[] args) throws Exception {
    Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler());
    ThreadContext.put("runId", UUID.randomUUID().toString());
    // System.out.println(args[0]);
    
    // printJsonObject(mainCalculateSingleReturn(args));

    // printJsonObject(mainCalculateReturnsAfterRefactor(args));
    // printJsonObject(mainReadQuotes(args));
  }
}
