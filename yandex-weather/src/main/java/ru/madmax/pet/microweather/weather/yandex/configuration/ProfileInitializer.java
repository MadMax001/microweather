package ru.madmax.pet.microweather.weather.yandex.configuration;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

public class ProfileInitializer implements ApplicationContextInitializer {
    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        String[] profiles = applicationContext
                .getEnvironment().getActiveProfiles();
        if (profiles.length == 0) {
            applicationContext.getEnvironment().setActiveProfiles("atOffice");
        }

    }
}
