package ru.madmax.pet.microweather.producer;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class PLauncherTest {
    private final ApplicationContext context;
    @Test
    void contextLoads() {
        assertThat(context.getBean("weatherFacadeService")).isNotNull();
    }

}
