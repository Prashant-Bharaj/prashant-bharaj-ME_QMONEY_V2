
package com.crio.warmup.stock.quotes;

import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.springframework.web.client.RestTemplate;

public class TiingoService implements StockQuotesService {
  static RestTemplate restTemplate;

  protected TiingoService(RestTemplate restTemplate) {
    TiingoService.restTemplate = restTemplate;
  }
  

  protected String buildUri(String symbol, LocalDate startDate, LocalDate endDate) {
    final String token = "7375d6e3553d0817d4ea50ee14460e4161354332";
    
    return String.format("https://api.tiingo.com/tiingo/daily/%s/prices?startDate=%s&endDate=%s&token=%s",
    symbol, startDate, endDate, token);
  }

  private static ObjectMapper getObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    return objectMapper;
  }

  public static List<Candle> getCandles(String url) throws JsonMappingException, JsonProcessingException{
    // RestTemplate restTemplate = new RestTemplate();
    // Candle[] candles = restTemplate.getForObject(url, TiingoCandle[].class);
    String response = restTemplate.getForObject(url, String.class);
    

    ObjectMapper objectMapper = getObjectMapper();
    Candle[] candles = objectMapper.readValue(response, TiingoCandle[].class);

    if(candles.length == 0) throw new RuntimeException();
    return Arrays.asList(candles);
  }

  @Override
  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to) throws JsonProcessingException {
    String url = buildUri(symbol, from, to);
    return getCandles(url);
  }


  // TODO: CRIO_TASK_MODULE_ADDITIONAL_REFACTOR
  //  Implement getStockQuote method below that was also declared in the interface.

  // Note:
  // 1. You can move the code from PortfolioManagerImpl#getStockQuote inside newly created method.
  // 2. Run the tests using command below and make sure it passes.
  //    ./gradlew test --tests TiingoServiceTest


  //CHECKSTYLE:OFF

  // TODO: CRIO_TASK_MODULE_ADDITIONAL_REFACTOR
  //  Write a method to create appropriate url to call the Tiingo API.

}
