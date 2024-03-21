package ru.madmax.pet.microweather.common.model;

import java.math.BigDecimal;

public interface ConversionRequest {
    Currency getBase();
    Currency getConvert();
    BigDecimal getAmount();

    void setBase(Currency base);
    void setConvert(Currency convert);
    void setAmount(BigDecimal amount);

}
