package ru.madmax.pet.microcurrency.consumer.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
@Table(name = "error", schema = "public")
public class ErrorEntity {
    private String id;
    private String details;
}
