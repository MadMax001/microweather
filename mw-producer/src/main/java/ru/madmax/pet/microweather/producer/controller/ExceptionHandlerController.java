package ru.madmax.pet.microweather.producer.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;
import ru.madmax.pet.microweather.producer.exception.WrongSourceException;

import java.util.stream.Collectors;

import static ru.madmax.pet.microweather.common.Constant.HEADER_REQUEST_ERROR_KEY;

@ControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class ExceptionHandlerController {
    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<Void>> handleValidationWebExchangeBindExceptions(
            WebExchangeBindException ex) {
        var errorMessage = ex.getBindingResult().getAllErrors()
                .stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.error(errorMessage);

        ResponseEntity<Void> responseEntity = ResponseEntity
                .badRequest()
                .header(HEADER_REQUEST_ERROR_KEY, errorMessage)
                .body(null);

        return Mono.just(responseEntity);
    }



    @ExceptionHandler(ServerWebInputException.class)
    public Mono<ResponseEntity<Void>> handleValidationServerWebInputExceptions(
            ServerWebInputException ex) {
        var errorMessage = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
        log.error(errorMessage);

        ResponseEntity<Void> responseEntity = ResponseEntity
                .badRequest()
                .header(HEADER_REQUEST_ERROR_KEY, errorMessage)
                .body(null);

        return Mono.just(responseEntity);
    }



    @ExceptionHandler(WrongSourceException.class)
    public Mono<ResponseEntity<Void>> handleWrongSourceException(WrongSourceException ex) {

        ResponseEntity<Void> responseEntity = ResponseEntity
                .badRequest()
                .header(HEADER_REQUEST_ERROR_KEY, ex.getMessage())
                .body(null);

        return Mono.just(responseEntity);
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<Void>> handleExceptions(Exception ex) {

        ResponseEntity<Void> responseEntity = ResponseEntity
                .internalServerError()
                .header(HEADER_REQUEST_ERROR_KEY, ex.getMessage())
                .body(null);

        return Mono.just(responseEntity);
    }


}
