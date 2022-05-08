package com.crio.warmup.stock.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GetClosePriceAndSymbol {
    private String symbol;
    private Double close;

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public Double getClose() {
        return close;
    }

    public void setClose(double close) {
        this.close = close;
    }
}