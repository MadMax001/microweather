package ru.madmax.pet.microcurrency.currate.configuration;

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
    private final Integer currencyRequestTimeout;

    public HttpClientConfiguration(@Value("${app.request.timeout}") Integer currencyRequestTimeout) {
        this.currencyRequestTimeout = currencyRequestTimeout;
    }

    @Bean
    public HttpClient httpClient() {
        return HttpClient
                .create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, currencyRequestTimeout)
                .doOnConnected(connection -> {
                    connection.addHandlerLast(
                            new ReadTimeoutHandler(currencyRequestTimeout, MILLISECONDS));
                    connection.addHandlerLast(
                            new WriteTimeoutHandler(currencyRequestTimeout, MILLISECONDS));
                });
    }

}
