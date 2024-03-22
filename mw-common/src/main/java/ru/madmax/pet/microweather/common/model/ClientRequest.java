package ru.madmax.pet.microweather.common.model;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

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
