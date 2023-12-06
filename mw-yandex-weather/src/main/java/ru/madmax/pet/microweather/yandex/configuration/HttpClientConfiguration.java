package ru.madmax.pet.microweather.yandex.configuration;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.EnableWebFlux;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.ProxyProvider;
import ru.madmax.pet.microweather.yandex.exception.AppYandexException;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import static java.util.concurrent.TimeUnit.*;

@Configuration
@EnableWebFlux
public class HttpClientConfiguration {

    private final Integer yaWeatherRequestTimeout;

    public HttpClientConfiguration(@Value("${app.weather.timeout}") Integer yaWeatherRequestTimeout) {
        this.yaWeatherRequestTimeout = yaWeatherRequestTimeout;
    }

    @Bean
    public HttpClient httpClient() {
        var httpClient = HttpClient
                .create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, yaWeatherRequestTimeout)
                .doOnConnected(connection -> {
                    connection.addHandlerLast(
                            new ReadTimeoutHandler(yaWeatherRequestTimeout, MILLISECONDS));
                    connection.addHandlerLast(
                            new WriteTimeoutHandler(yaWeatherRequestTimeout, MILLISECONDS));
                });
        if (isNeedProxy()) {
            httpClient
                .proxy(typeSpec -> typeSpec
                        .type(ProxyProvider.Proxy.HTTP)
                        .host("http://10.73.248.6")
                        .port(3128)
                        .username("073BodrovMB")
                        .password(pwd -> "vCglcBZ71"));

        }
        return httpClient;
    }

    private boolean isNeedProxy() {
        return getAllIP().stream().anyMatch(ip -> ip.startsWith("10.73"));
    }

    private List<String> getAllIP() {
        List<String> ipList = new ArrayList<>() ;
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (!iface.isLoopback()  && iface.isUp()) {
                    Enumeration<InetAddress> addresses = iface.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        InetAddress addr = addresses.nextElement();
                        ipList.add(addr.getHostAddress());
                    }
                }
            }
        } catch (SocketException e) {
            throw new AppYandexException(e);
        }
        return ipList;
    }

}
