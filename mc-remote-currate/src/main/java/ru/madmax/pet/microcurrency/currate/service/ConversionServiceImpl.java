package ru.madmax.pet.microcurrency.currate.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import ru.madmax.pet.microcurrency.currate.exception.IllegalAmountException;
import ru.madmax.pet.microcurrency.currate.exception.IllegalRateException;

public class ConversionServiceImpl implements ConversionService {
    private static final int SCALE = 4;

    @Override
    public BigDecimal covert(BigDecimal amount, BigDecimal rate)
            throws IllegalAmountException, IllegalRateException{
        if (amount == null) {
            throw new IllegalAmountException("The amount is not set");
        }
        if (rate == null) {
            throw new IllegalRateException("The rate is not defined");
        }
        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalAmountException("The amount is negative");
        }
        if (rate.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalRateException("The rate is zero");
        }
        if (rate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalRateException("The rate is negative");
        }

        return amount.divide(rate, SCALE, RoundingMode.HALF_UP);
    }
}
