
package com.crio.warmup.stock;

// import sun.jvm.hotspot.debugger.ThreadContext;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.border.EtchedBorder;

import com.crio.warmup.stock.dto.GetClosePriceAndSymbol;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestTemplate;

public class PortfolioManagerApplication {

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

  // TODO: CRIO_TASK_MODULE_REST_API
  // Find out the closing price of each stock on the end_date and return the list
  // of all symbols in ascending order by its close value on end date.

  // Note:
  // 1. You may have to register on Tiingo to get the api_token.
  // 2. Look at args parameter and the module instructions carefully.
  // 2. You can copy relevant code from #mainReadFile to parse the Json.
  // 3. Use RestTemplate#getForObject in order to call the API,
  // and deserialize the results in List<Candle>

  public static List<String> mainReadQuotes(String[] args) throws IOException, URISyntaxException {
    // Parse the string to LocalDate
    LocalDate ld = LocalDate.parse(args[1]);
    RestTemplate restTemplate = new RestTemplate();
    List<PortfolioTrade> portfolioTrades = readTradesFromJson(args[0]);
    // for each portfolio trade get the closing price and sort according to that
    List<TotalReturnsDto> totalReturnsDtos = new ArrayList<>();
    for (PortfolioTrade portfolioTrade : portfolioTrades) {
      String url = prepareUrl(portfolioTrade, ld, "7375d6e3553d0817d4ea50ee14460e4161354332");
      url += "&sort=-date";
      TiingoCandle[] tiingoCandles = restTemplate.getForObject(url, TiingoCandle[].class);
      if(tiingoCandles.length == 0) throw new RuntimeException();
      for(TiingoCandle tiingoCandle : tiingoCandles){
        if(tiingoCandle.getClose() != null){
          totalReturnsDtos.add(new TotalReturnsDto(portfolioTrade.getSymbol(), tiingoCandle.getClose()));
          break;
        }
      }
    }
    // now sort the Trades according to trade close
    Collections.sort(totalReturnsDtos, new Comparator<TotalReturnsDto>(){
      @Override
      public int compare(TotalReturnsDto t1, TotalReturnsDto t2){
        if(t1.getClose() > t2.getClose()){
          return 1;
        } else if(t1.getClose() < t2. getClose()){
          return -1;
        } else {
          return 0;
        }
      }
    });
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
    String resultOfResolveFilePathArgs0 = "";
    String toStringOfObjectMapper = "";
    String functionNameFromTestFileInStackTrace = "";
    String lineNumberFromTestFileInStackTrace = "";

    return Arrays.asList(new String[] { valueOfArgument0, resultOfResolveFilePathArgs0, toStringOfObjectMapper,
        functionNameFromTestFileInStackTrace, lineNumberFromTestFileInStackTrace });
  }

  public static void main(String[] args) throws Exception {
    // Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler());
    // ThreadContext.put("runId", UUID.randomUUID().toString());
    System.out.println(args[0]);
    printJsonObject(mainReadQuotes(args));
  }
}
