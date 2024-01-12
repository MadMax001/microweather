package ru.madmax.pet.microweather.consumer.model;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.With;
import ru.madmax.pet.microweather.common.model.TestBuilder;

@AllArgsConstructor
@NoArgsConstructor(staticName = "anErrorDomain")
@With
public class TestErrorDomain implements TestBuilder<ErrorDomain> {
    private String id = "test_domain_key";
    private String details = "error";
    @Override
    public ErrorDomain build() {
        var error = new ErrorDomain();
        error.setId(id);
        error.setDetails(details);
        return error;
    }
}
