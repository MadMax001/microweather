package ru.madmax.pet.microweather.producer.configuration;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

@Component
@ConfigurationProperties(prefix = "app.weather.services")
public class WeatherRemoteServicesListBuilder {
    private List<RemoteService> services;
    private Map<String, URL> map;

    @PostConstruct
    private void initMap() throws MalformedURLException {
        map = new HashMap<>();
        for (RemoteService service : services) {
            map.put(service.getId(), new URL(String.format("%s://%s",
                    service.getUrl(),
                    service.getPath())));
        }
    }

    @Setter
    @Getter
    public static class RemoteService {
        private String id;
        private String url;
        private String path;
    }

    public URL getURLByKey(String key) {
        return Optional.ofNullable(map.get(key))
                .orElseThrow(() -> new NoSuchElementException(key));
    }

}
