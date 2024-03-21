package ru.madmax.pet.microcurrency.currate.service;

import ru.madmax.pet.microcurrency.currate.exception.IllegalAmountException;
import ru.madmax.pet.microcurrency.currate.exception.IllegalRateException;

import java.math.BigDecimal;

public interface ConversionService {
    BigDecimal covert(BigDecimal amount, BigDecimal rate) throws IllegalAmountException, IllegalRateException;
}
