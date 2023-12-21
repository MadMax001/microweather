package ru.madmax.pet.microweather.yandex.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

@ControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class ExceptionHandlerController {
    private static final String REQUEST_GUID_HEADER_NAME = "request-guid";
    private static final String REQUEST_ERROR_HEADER_NAME = "request-error";
    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<Void>> handleValidationWebExchangeBindExceptions(
            WebExchangeBindException ex,
            @RequestHeader(name= REQUEST_GUID_HEADER_NAME, required = false) String requestGuid) {
        var errorMessage = ex.getBindingResult().getAllErrors()
                .stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.error(errorMessage);

        HttpHeaders headers = new HttpHeaders();
        headers.add(REQUEST_ERROR_HEADER_NAME, errorMessage);
        if (requestGuid != null)
            headers.add(REQUEST_GUID_HEADER_NAME, requestGuid);

        ResponseEntity<Void> responseEntity = ResponseEntity
                .badRequest()
                .headers(headers)
                .body(null);

        return Mono.just(responseEntity);
    }


    @ExceptionHandler(ServerWebInputException.class)
    public Mono<ResponseEntity<Void>> handleValidationServerWebInputExceptions(
            ServerWebInputException ex,
            @RequestHeader(name= REQUEST_GUID_HEADER_NAME, required = false) String requestGuid) {
        var errorMessage = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
        log.error(errorMessage);

        HttpHeaders headers = new HttpHeaders();
        headers.add(REQUEST_ERROR_HEADER_NAME, errorMessage);
        if (requestGuid != null)
            headers.add(REQUEST_GUID_HEADER_NAME, requestGuid);

        ResponseEntity<Void> responseEntity = ResponseEntity
                .badRequest()
                .headers(headers)
                .body(null);

        return Mono.just(responseEntity);
    }


    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<Void>> handleExceptions(
            Exception ex,
            @RequestHeader(name= REQUEST_GUID_HEADER_NAME, required = false) String requestGuid) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(REQUEST_ERROR_HEADER_NAME, ex.getMessage());
        if (requestGuid != null)
            headers.add(REQUEST_GUID_HEADER_NAME, requestGuid);

        ResponseEntity<Void> responseEntity = ResponseEntity
                .internalServerError()
                .headers(headers)
                .body(null);

        return Mono.just(responseEntity);
    }


}
