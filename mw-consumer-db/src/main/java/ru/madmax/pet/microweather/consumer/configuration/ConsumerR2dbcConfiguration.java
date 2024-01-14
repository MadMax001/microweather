package ru.madmax.pet.microweather.consumer.configuration;

import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.stereotype.Component;
import ru.madmax.pet.microweather.consumer.configuration.converter.model.WeatherReaderConverter;
import ru.madmax.pet.microweather.consumer.configuration.converter.model.WeatherWriterConverter;

import java.util.ArrayList;
import java.util.List;

@Component
public class ConsumerR2dbcConfiguration extends AbstractR2dbcConfiguration {
    @Value("${spring.r2dbc.url}")
    private String url;

    public ConnectionFactory connectionFactory() {
        return ConnectionFactories.get(url);
    }

    @Override
    protected List<Object> getCustomConverters() {

        List<Object> converterList = new ArrayList<>();
        converterList.add(new WeatherReaderConverter());
        converterList.add(new WeatherWriterConverter());
        return converterList;
    }
}
