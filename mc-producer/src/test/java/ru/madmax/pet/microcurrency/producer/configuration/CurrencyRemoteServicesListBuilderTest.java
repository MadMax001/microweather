package ru.madmax.pet.microcurrency.producer.configuration;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import ru.madmax.pet.microcurrency.producer.exception.WrongSourceException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;


@ExtendWith(SpringExtension.class)
@ContextConfiguration(initializers = ConfigDataApplicationContextInitializer.class)
@EnableConfigurationProperties(value = CurrencyRemoteServicesListBuilder.class)
@ActiveProfiles("test")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class CurrencyRemoteServicesListBuilderTest {
    private final CurrencyRemoteServicesListBuilder servicesBuilder;

    @Test
    void checkForRemoteServiceMap() {
        var firstObject = servicesBuilder.getURLByKey("first");
        assertThat(firstObject).isNotNull();
        assertThat(firstObject.getHost()).isEqualTo("value1.ru");
        assertThat(firstObject.getPath()).isEqualTo("/value2");
        assertThat(firstObject.getProtocol()).isEqualTo("http");

        var secondObject = servicesBuilder.getURLByKey("second");
        assertThat(secondObject).isNotNull();
        assertThat(secondObject.getHost()).isEqualTo("value3.org");
        assertThat(secondObject.getPath()).isEqualTo("/value4");
        assertThat(secondObject.getProtocol()).isEqualTo("https");
    }

    @Test
    void checkForNonexistingServiceInMap() {
        assertThrows(WrongSourceException.class, () -> servicesBuilder.getURLByKey("123"));
    }
}