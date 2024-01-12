package ru.madmax.pet.microweather.consumer.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Setter
@NoArgsConstructor
@Table(name = "error")
public class ErrorDomain {
    private String id;
    private String details;
}
