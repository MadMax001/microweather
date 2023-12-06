package ru.madmax.pet.microweather.common.model;

import lombok.Data;

@Data
public class MessageDTO {
    private MessageType type;
    private String message;
}
