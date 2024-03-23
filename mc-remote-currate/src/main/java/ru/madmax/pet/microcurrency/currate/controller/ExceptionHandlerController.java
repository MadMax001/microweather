package ru.madmax.pet.microcurrency.currate.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

import static ru.madmax.pet.microcurrency.common.model.Constant.HEADER_REQUEST_ERROR_KEY;
import static ru.madmax.pet.microcurrency.common.model.Constant.HEADER_REQUEST_GUID_KEY;


@ControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class ExceptionHandlerController {

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<Void>> handleValidationWebExchangeBindExceptions(
            WebExchangeBindException ex,
            @RequestHeader(name = HEADER_REQUEST_GUID_KEY, required = false) String requestGuid) {
        var errorMessage = ex.getBindingResult().getAllErrors()
                .stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.error(errorMessage);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HEADER_REQUEST_ERROR_KEY, errorMessage);
        if (requestGuid != null)
            headers.add(HEADER_REQUEST_GUID_KEY, requestGuid);

        ResponseEntity<Void> responseEntity = ResponseEntity
                .badRequest()
                .headers(headers)
                .body(null);

        return Mono.just(responseEntity);
    }


    @ExceptionHandler(ServerWebInputException.class)
    public Mono<ResponseEntity<Void>> handleValidationServerWebInputExceptions(
            ServerWebInputException ex,
            @RequestHeader(name= HEADER_REQUEST_GUID_KEY, required = false) String requestGuid) {
        var errorMessage = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
        log.error(errorMessage);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HEADER_REQUEST_ERROR_KEY, errorMessage);
        if (requestGuid != null)
            headers.add(HEADER_REQUEST_GUID_KEY, requestGuid);

        ResponseEntity<Void> responseEntity = ResponseEntity
                .badRequest()
                .headers(headers)
                .body(null);

        return Mono.just(responseEntity);
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<Void>> handleExceptions(
            Exception ex,
            @RequestHeader(name= HEADER_REQUEST_GUID_KEY, required = false) String requestGuid) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HEADER_REQUEST_ERROR_KEY, ex.getMessage());
        if (requestGuid != null)
            headers.add(HEADER_REQUEST_GUID_KEY, requestGuid);

        ResponseEntity<Void> responseEntity = ResponseEntity
                .internalServerError()
                .headers(headers)
                .body(null);

        return Mono.just(responseEntity);
    }


}
