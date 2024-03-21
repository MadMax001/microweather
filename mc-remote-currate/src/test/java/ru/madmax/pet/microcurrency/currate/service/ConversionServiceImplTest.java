package ru.madmax.pet.microcurrency.currate.service;

import org.junit.jupiter.api.Test;
import ru.madmax.pet.microcurrency.currate.exception.IllegalAmountException;
import ru.madmax.pet.microcurrency.currate.exception.IllegalRateException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConversionServiceImplTest {
    ConversionService service = new ConversionServiceImpl();
    @Test
    void convertAmountByRate() throws IllegalAmountException, IllegalRateException {
        BigDecimal amount = new BigDecimal("10000");
        BigDecimal rate = new BigDecimal("313");
        BigDecimal expected = new BigDecimal("31.9489");
        BigDecimal result = service.covert(amount, rate);
        assertThat(result).isEqualByComparingTo(expected);
    }

    @Test
    void convertAmount_WhenRateIsNull_ThrowException() throws IllegalAmountException, IllegalRateException {
        BigDecimal amount = new BigDecimal("10000");
        BigDecimal rate = null;
        assertThatThrownBy(() -> service.covert(amount, rate))
                .isInstanceOf(IllegalRateException.class)
                .hasMessageStartingWith("The rate is not defined");

    }

    @Test
    void convertAmount_WhenRateIsZero_ThrowException() {
        BigDecimal amount = new BigDecimal("10000");
        BigDecimal rate = BigDecimal.ZERO;
        assertThatThrownBy(() -> service.covert(amount, rate))
                .isInstanceOf(IllegalRateException.class)
                .hasMessageStartingWith("The rate is zero");

    }

    @Test
    void convertAmount_WhenRateIsNegative_ThrowException() {
        BigDecimal amount = new BigDecimal("10000");
        BigDecimal rate = new BigDecimal("-10");
        assertThatThrownBy(() -> service.covert(amount, rate))
                .isInstanceOf(IllegalRateException.class)
                .hasMessageStartingWith("The rate is negative");

    }

    @Test
    void convertNullAmount_ThrowException() {
        BigDecimal amount = null;
        BigDecimal rate = new BigDecimal("313");
        assertThatThrownBy(() -> service.covert(amount, rate))
                .isInstanceOf(IllegalAmountException.class)
                .hasMessageStartingWith("The amount is not set");
    }

    @Test
    void convertNegativeAmount_ThrowException() {
        BigDecimal amount = new BigDecimal("-10");
        BigDecimal rate = new BigDecimal("313");
        assertThatThrownBy(() -> service.covert(amount, rate))
                .isInstanceOf(IllegalAmountException.class)
                .hasMessageStartingWith("The amount is negative");
    }

    @Test
    void convertZeroAmount_AndGetZeroResult() throws IllegalAmountException, IllegalRateException {
        BigDecimal amount = new BigDecimal("0");
        BigDecimal rate = new BigDecimal("313");
        BigDecimal expected = BigDecimal.ZERO;
        BigDecimal result = service.covert(amount, rate);
        assertThat(result).isEqualByComparingTo(expected);
    }

}