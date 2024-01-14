package ru.madmax.pet.microweather.consumer.configuration.converter.model;

import io.r2dbc.spi.Row;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import ru.madmax.pet.microweather.common.model.Fact;
import ru.madmax.pet.microweather.common.model.Info;
import ru.madmax.pet.microweather.consumer.model.WeatherDomain;

@ReadingConverter
public class WeatherReaderConverter implements Converter<Row, WeatherDomain> {
    @Override
    public WeatherDomain convert(Row source) {
        WeatherDomain wd = new WeatherDomain();
        Fact fact = new Fact();
        fact.setTemp(source.get("temperature", Double.class));
        fact.setWindSpeed(source.get("wind", Double.class));

        Info info = new Info();
        info.setUrl(source.get("url", String.class));

        wd.setFact(fact);
        wd.setInfo(info);
        wd.setNow(source.get("now", Long.class));
        wd.setId(source.get("id", String.class));

        return wd;
    }
}
