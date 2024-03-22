package ru.madmax.pet.microcurrency.producer.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import ru.madmax.pet.microcurrency.producer.service.CurrencyService;
import ru.madmax.pet.microcurrency.producer.model.ClientRequest;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1")
@Slf4j
public class ProducerControllerV1 {
    private final CurrencyService currencyService;

    @PostMapping("/register")
    public Mono<ResponseEntity<String>> weatherRequest(@RequestBody @Valid ClientRequest request) {
        return currencyService.registerRequest(request)
        .map(guid ->
                ResponseEntity
                        .ok()
                        .body(guid)
        );
    }
}
