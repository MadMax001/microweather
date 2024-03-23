package ru.madmax.pet.microcurrency.consumer.model;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.With;
import ru.madmax.pet.microcurrency.common.model.Currency;
import ru.madmax.pet.microcurrency.common.model.TestBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;

@AllArgsConstructor
@NoArgsConstructor(staticName = "aConversionEntity")
@With
public class TestConversionEntityBuilder implements TestBuilder<ConversionEntity> {
    private String id = "test_domain_key";
    private Currency base = Currency.RUB;
    private Currency convert = Currency.USD;
    private BigDecimal baseAmount = new BigDecimal(10000);
    private BigDecimal convertAmount = new BigDecimal("155.8060").setScale(4, RoundingMode.DOWN);
    private Long sourceId = 1L;

    @Override
    public ConversionEntity build() {
        ConversionEntity entity = new ConversionEntity();
        entity.setId(id);
        entity.setBase(base);
        entity.setConvert(convert);
        entity.setBaseAmount(baseAmount);
        entity.setConversionAmount(convertAmount);
        entity.setSourceId(sourceId);
        return entity;
    }
}
