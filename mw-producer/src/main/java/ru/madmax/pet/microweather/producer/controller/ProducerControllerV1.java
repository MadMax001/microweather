package ru.madmax.pet.microweather.producer.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import ru.madmax.pet.microweather.producer.model.RequestDTO;
import ru.madmax.pet.microweather.producer.service.WeatherService;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1")
@Slf4j
public class ProducerControllerV1 {
    private final WeatherService weatherService;

    @PostMapping("/register")
    public Mono<ResponseEntity<String>> weatherRequest(@RequestBody @Valid RequestDTO requestDTO) {
        return weatherService.registerRequest(requestDTO)
        .map(guid ->
                ResponseEntity
                        .ok()
                        .body(guid)
        );
    }
}
