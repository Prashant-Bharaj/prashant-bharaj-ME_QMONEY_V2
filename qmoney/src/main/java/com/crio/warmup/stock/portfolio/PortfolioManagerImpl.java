
package com.crio.warmup.stock.portfolio;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.SECONDS;

import com.crio.warmup.stock.dto.AnnualizedReturn;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.springframework.web.client.RestTemplate;

public class PortfolioManagerImpl implements PortfolioManager {



  static RestTemplate restTemplate;
  // Caution: Do not delete or modify the constructor, or else your build will break!
  // This is absolutely necessary for backward compatibility
  protected PortfolioManagerImpl(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }


  //TODO: CRIO_TASK_MODULE_REFACTOR
  // 1. Now we want to convert our code into a module, so we will not call it from main anymore.
  //    Copy your code from Module#3 PortfolioManagerApplication#calculateAnnualizedReturn
  //    into #calculateAnnualizedReturn function here and ensure it follows the method signature.
  // 2. Logic to read Json file and convert them into Objects will not be required further as our
  //    clients will take care of it, going forward.

  // Note:
  // Make sure to exercise the tests inside PortfolioManagerTest using command below:
  // ./gradlew test --tests PortfolioManagerTest

  //CHECKSTYLE:OFF




  private Comparator<AnnualizedReturn> getComparator() {
    return Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed();
  }

  //CHECKSTYLE:OFF

  // TODO: CRIO_TASK_MODULE_REFACTOR
  //  Extract the logic to call Tiingo third-party APIs to a separate function.
  //  Remember to fill out the buildUri function and use that.

  public static List<Candle> getCandles(String url){
    // RestTemplate restTemplate = new RestTemplate();
    Candle[] candles = restTemplate.getForObject(url, TiingoCandle[].class);
    if(candles.length == 0) throw new RuntimeException();
    return Arrays.asList(candles);
  }

  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to) {
     String url = buildUri(symbol, from, to);
     return getCandles(url);
  }

  protected String buildUri(String symbol, LocalDate startDate, LocalDate endDate) {
    final String token = "7375d6e3553d0817d4ea50ee14460e4161354332";
    // String uriTemplate = "https:api.tiingo.com/tiingo/daily/$SYMBOL/prices?"
    //     + "startDate=$STARTDATE&endDate=$ENDDATE&token=$APIKEY";
    // uriTemplate.replace("$SYMBOL", symbol)
    //           .replace("$STARTDATE", startDate.toString())
    //           .replace("$ENDDATE", endDate.toString())
    //           .replace("$APIKEY", token);
    // return uriTemplate;
    return String.format("https://api.tiingo.com/tiingo/daily/%s/prices?startDate=%s&endDate=%s&token=%s",
    symbol, startDate, endDate, token);
  }

  
  public static AnnualizedReturn calculateSingleAnnualizedReturns(LocalDate endDate,
      PortfolioTrade trade, Double buyPrice, Double sellPrice) {
        Double totalReturn = (sellPrice - buyPrice) / (buyPrice * 1.0);

        Double year = trade.getPurchaseDate().until(endDate, ChronoUnit.DAYS)/365.24;
        Double annualizedReturns = Math.pow((1 + totalReturn), ((1.0) / (year*1.0))) - 1;

      return new AnnualizedReturn(trade.getSymbol(), annualizedReturns, totalReturn);
  }

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

  @Override
  public List<AnnualizedReturn> calculateAnnualizedReturn(List<PortfolioTrade> portfolioTrades, LocalDate endDate) {
      // get buy and sell price
      List<AnnualizedReturn> annualizedReturns = new ArrayList<>();
      for(PortfolioTrade portfolioTrade : portfolioTrades){
        List<Candle> candles = getStockQuote(portfolioTrade.getSymbol(), portfolioTrade.getPurchaseDate(), endDate);
        Double buyPrice = getOpeningPriceOnStartDate(candles);
        Double sellPrice = getClosingPriceOnEndDate(candles);
        annualizedReturns.add(calculateSingleAnnualizedReturns(endDate, portfolioTrade, buyPrice, sellPrice));
      }
      return annualizedReturns
              .stream()
              .sorted((a,b)->b.getAnnualizedReturn().compareTo(a.getAnnualizedReturn()))
              .collect(Collectors.toList());
  }
}
