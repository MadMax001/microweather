package ru.madmax.pet.microcurrency.producer.model;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.madmax.pet.microweather.common.model.ServiceRequest;

@Data
@EqualsAndHashCode(callSuper = true)
public class ClientRequest extends ServiceRequest {
    @NotNull(message = "Source is not defined")
    private String source;

    @Override
    public String toString() {
        return  this.getSource() +
                " " +
                super.toString();
    }

}
