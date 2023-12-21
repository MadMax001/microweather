package ru.madmax.pet.microweather.producer.configuration;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import ru.madmax.pet.microweather.producer.exception.AppProducerException;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

@Configuration
@ConfigurationProperties(prefix = "app.weather")
public class WeatherRemoteServicesListBuilder {
    private Map<String, URL> map;

    public void setServices(List<RemoteServiceUrl> services) {
        map = services.stream().collect(Collectors.toMap(
                RemoteServiceUrl::id,
                element -> {
                    try {
                        return new URL(String.format("%s%s",
                                element.host(),
                                (element.path().startsWith("/")?
                                    element.path():
                                    "/" + element.path())));
                    } catch (MalformedURLException e) {
                        throw new AppProducerException(e);
                    }
                },
                (e1,e2) -> {throw new AppProducerException("Duplicate service id in configuration");}));
    }

    public record RemoteServiceUrl(String id, String host, String path) {}

    public URL getURLByKey(String key) {
        return Optional.ofNullable(map.get(key))
                .orElseThrow(() -> new NoSuchElementException(key));
    }

}
