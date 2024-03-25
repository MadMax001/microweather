package ru.madmax.pet.microcurrency.common.model;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;

public enum Currency {
    RUB,
    USD,
    CAD;

    private static final Map<String, Currency> map;
    static {
        map = Arrays.stream(Currency.values()).sequential().collect(Collectors.toMap(
                Enum::name,
                identity())
        );
    }

    public static Currency getBy(String str) {
        return map.get(str);
    }
}
