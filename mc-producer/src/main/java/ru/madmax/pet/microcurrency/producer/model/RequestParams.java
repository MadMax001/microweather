package ru.madmax.pet.microcurrency.producer.model;

import lombok.Builder;
import lombok.Getter;

import java.net.URL;

@Builder
@Getter
public class RequestParams {
    private final URL url;
    private final String guid;
}
