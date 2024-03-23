package ru.madmax.pet.microcurrency.consumer.model;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.With;
import ru.madmax.pet.microcurrency.common.model.TestBuilder;

@AllArgsConstructor
@NoArgsConstructor(staticName = "anErrorDomain")
@With
public class TestErrorDomainBuilder implements TestBuilder<ErrorEntity> {
    private String id = "test_domain_key";
    private String details = "error";
    @Override
    public ErrorEntity build() {
        var error = new ErrorEntity();
        error.setId(id);
        error.setDetails(details);
        return error;
    }
}
