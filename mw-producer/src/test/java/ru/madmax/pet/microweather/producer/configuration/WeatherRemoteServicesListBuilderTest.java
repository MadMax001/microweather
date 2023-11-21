package ru.madmax.pet.microweather.producer.configuration;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;


@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {"app.weather.timeout=1000"})
@ContextConfiguration(classes = HttpClientConfiguration.class)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class WeatherRemoteServicesListBuilderTest {
    private final WeatherRemoteServicesListBuilder servicesBuilder;

}