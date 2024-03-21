package ru.madmax.pet.microcurrency.producer.model;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.madmax.pet.microweather.common.model.CurrencyRequest;

@Data
@EqualsAndHashCode(callSuper = true)
public class CurrencyRequestX extends CurrencyRequest {
    @NotNull(message = "Source is not defined")
    private String source;

    @Override
    public String toString() {
        return "Request {source=" + this.getSource() + " " + super.toString() + "}";
    }
}
