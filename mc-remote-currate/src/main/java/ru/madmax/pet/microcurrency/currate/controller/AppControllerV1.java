package ru.madmax.pet.microcurrency.currate.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import ru.madmax.pet.microcurrency.currate.service.CurrencyService;
import ru.madmax.pet.microcurrency.common.model.ServiceRequest;
import ru.madmax.pet.microcurrency.common.model.Conversion;

import static ru.madmax.pet.microcurrency.common.model.Constant.HEADER_REQUEST_ERROR_KEY;
import static ru.madmax.pet.microcurrency.common.model.Constant.HEADER_REQUEST_GUID_KEY;


@RestController
@AllArgsConstructor
@RequestMapping("/api/v1")
@Slf4j
public class AppControllerV1 {
    private final CurrencyService currencyService;

    @PostMapping("/convert")
    public Mono<ResponseEntity<Conversion>> conversionRequest(@RequestBody @Valid ServiceRequest request,
                                                           @RequestHeader(name= HEADER_REQUEST_GUID_KEY) String requestGuid) {

        var mono = currencyService.getRateMono(request);
        return mono
                .map(result ->
                        ResponseEntity
                                .ok()
                                .header(HEADER_REQUEST_GUID_KEY, requestGuid)
                                .body(result)
                )
                .onErrorResume(error->
                        Mono.just(ResponseEntity
                                .internalServerError()
                                .header(HEADER_REQUEST_GUID_KEY, requestGuid)
                                .header(HEADER_REQUEST_ERROR_KEY, error.getMessage())
                                .body(null))
                );
    }


}
