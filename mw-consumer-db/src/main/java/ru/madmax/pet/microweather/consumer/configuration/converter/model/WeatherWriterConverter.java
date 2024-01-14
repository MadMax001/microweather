package ru.madmax.pet.microweather.consumer.configuration.converter.model;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.r2dbc.mapping.OutboundRow;
import org.springframework.r2dbc.core.Parameter;
import ru.madmax.pet.microweather.consumer.model.WeatherDomain;

@WritingConverter
public class WeatherWriterConverter implements Converter<WeatherDomain, OutboundRow> {
    @Override
    public OutboundRow convert(WeatherDomain source) {
        OutboundRow row = new OutboundRow();
        row.put("id", Parameter.from(source.getId()));
        row.put("now", Parameter.from(source.getNow()));
        row.put("temperature", Parameter.from(source.getFact().getTemp()));
        row.put("wind", Parameter.from(source.getFact().getWindSpeed()));
        row.put("url", Parameter.from(source.getInfo().getUrl()));

        return row;
    }
}
