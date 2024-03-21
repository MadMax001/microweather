package ru.madmax.pet.microweather.common.model;

import java.math.BigDecimal;

public interface ConversionResponse {
    Currency getFrom();
    Currency getTo();
    BigDecimal getRate();
    String getSource();
    BigDecimal getAmount();

    void setFrom(Currency from);
    void setTo(Currency to);
    void setRate(BigDecimal rate);
    void setSource(String source);
    void setAmount(BigDecimal amount);
}
