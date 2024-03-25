package ru.madmax.pet.microcurrency.common.model;

import lombok.Data;

@Data
public class MessageDTO {
    private MessageType type;
    private String message;
}
