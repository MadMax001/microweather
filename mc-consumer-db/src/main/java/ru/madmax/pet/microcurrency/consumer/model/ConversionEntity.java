package ru.madmax.pet.microcurrency.consumer.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.relational.core.mapping.Table;
import ru.madmax.pet.microcurrency.common.model.Currency;

import java.math.BigDecimal;

@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
@Table(name = "conversion", schema = "public")
public class ConversionEntity {

    private String id;
    private Currency base;
    private Currency convert;
    private BigDecimal baseAmount;
    private BigDecimal conversionAmount;
    private Long sourceId;

}
