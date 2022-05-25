
package com.crio.warmup.stock.quotes;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.SECONDS;

import com.crio.warmup.stock.dto.AlphavantageCandle;
import com.crio.warmup.stock.dto.AlphavantageDailyResponse;
import com.crio.warmup.stock.dto.Candle;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class AlphavantageService implements StockQuotesService {

  
  static RestTemplate restTemplate;

  protected AlphavantageService(RestTemplate restTemplate) {
    AlphavantageService.restTemplate = restTemplate;
  }
  

  protected String buildUri(String symbol) {
    final String token = "QIX9YAXZUFDYAL5H";
    
    return String.format("https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol=%s&outputsize=full&apikey=%s", symbol, token);
  }

  public static List<Candle> getCandles(String url) throws JsonMappingException, JsonProcessingException{

    String response = restTemplate.getForObject(url, String.class);


    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());

    AlphavantageDailyResponse alphavantageDailyResponses = objectMapper.readValue(response, AlphavantageDailyResponse.class);

    List<Candle> listOfCandles = new ArrayList<>();
    for(Map.Entry<LocalDate, AlphavantageCandle> entry : alphavantageDailyResponses.getCandles().entrySet()){
      LocalDate date = entry.getKey();
      AlphavantageCandle candle = entry.getValue();
      candle.setDate(date);
      listOfCandles.add(candle);
    }
    return listOfCandles;
  }

  @Override
  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to) throws JsonProcessingException {
    String url = buildUri(symbol);
    List<Candle> candles = getCandles(url);
    candles = candles.stream().filter(e -> (e.getDate().isAfter(from) && e.getDate().isBefore(to.plusDays(1) ) ) ).sorted((e1, e2)->e1.getDate().compareTo(e2.getDate())).collect(Collectors.toList());
    return candles;
  }

}

  // TODO: CRIO_TASK_MODULE_ADDITIONAL_REFACTOR
  //  Implement the StockQuoteService interface as per the contracts. Call Alphavantage service
  //  to fetch daily adjusted data for last 20 years.
  //  Refer to documentation here: https://www.alphavantage.co/documentation/
  //  --
  //  The implementation of this functions will be doing following tasks:
  //    1. Build the appropriate url to communicate with third-party.
  //       The url should consider startDate and endDate if it is supported by the provider.
  //    2. Perform third-party communication with the url prepared in step#1
  //    3. Map the response and convert the same to List<Candle>
  //    4. If the provider does not support startDate and endDate, then the implementation
  //       should also filter the dates based on startDate and endDate. Make sure that
  //       result contains the records for for startDate and endDate after filtering.
  //    5. Return a sorted List<Candle> sorted ascending based on Candle#getDate
  //  IMP: Do remember to write readable and maintainable code, There will be few functions like
  //    Checking if given date falls within provided date range, etc.
  //    Make sure that you write Unit tests for all such functions.
  //  Note:
  //  1. Make sure you use {RestTemplate#getForObject(URI, String)} else the test will fail.
  //  2. Run the tests using command below and make sure it passes:
  //    ./gradlew test --tests AlphavantageServiceTest
  //CHECKSTYLE:OFF
    //CHECKSTYLE:ON
  // TODO: CRIO_TASK_MODULE_ADDITIONAL_REFACTOR
  //  1. Write a method to create appropriate url to call Alphavantage service. The method should
  //     be using configurations provided in the {@link @application.properties}.
  //  2. Use this method in #getStockQuote.
