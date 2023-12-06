package ru.madmax.pet.microweather.common.model;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.With;

@AllArgsConstructor
@NoArgsConstructor(staticName = "aMessageDTO")
@With
public class TestMessageDTOBuilder implements TestBuilder<MessageDTO>{
    private MessageType type = MessageType.WEATHER;
    private String message = "";


    @Override
    public MessageDTO build() {
        var messageDTO = new MessageDTO();
        messageDTO.setType(type);
        messageDTO.setMessage(message);
        return messageDTO;
    }
}
