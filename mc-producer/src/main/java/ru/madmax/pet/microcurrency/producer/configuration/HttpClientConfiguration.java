package ru.madmax.pet.microcurrency.producer.configuration;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.EnableWebFlux;
import reactor.netty.http.client.HttpClient;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

@Configuration
@EnableWebFlux
public class HttpClientConfiguration {

    private final Integer requestTimeout;

    public HttpClientConfiguration(@Value("${app.request.timeout}") Integer requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    @Bean
    public HttpClient httpClient() {
        return HttpClient
                .create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, requestTimeout)
                .doOnConnected(connection -> {
                    connection.addHandlerLast(
                            new ReadTimeoutHandler(requestTimeout, MILLISECONDS));
                    connection.addHandlerLast(
                            new WriteTimeoutHandler(requestTimeout, MILLISECONDS));
                });
    }

}
