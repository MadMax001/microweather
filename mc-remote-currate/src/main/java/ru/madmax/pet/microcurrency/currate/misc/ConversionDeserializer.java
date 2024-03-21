package ru.madmax.pet.microcurrency.currate.misc;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import ru.madmax.pet.microcurrency.currate.exception.IllegalModelStructureException;
import ru.madmax.pet.microcurrency.currate.model.ConversionResponseX;
import ru.madmax.pet.microweather.common.model.ConversionResponse;
import ru.madmax.pet.microweather.common.model.Currency;

import java.io.IOException;
import java.math.BigDecimal;

public class ConversionDeserializer  extends StdDeserializer<ConversionResponse> {
    private static final String STATUS_KEY = "status";
    public ConversionDeserializer() {
        this(null);
    }

    public ConversionDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public ConversionResponse deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
            throws IOException {

        JsonNode node = jsonParser.getCodec().readTree(jsonParser);

        int code = getStatus(node);
        if (code == 200) {
            return parseDataBlock(node);
        } else {
            throw new IllegalModelStructureException("Wrong answer code", "" + code);
        }
    }

    private int getStatus(JsonNode node ) {
        try {
            return Integer.parseInt(node.get(STATUS_KEY).asText());
        } catch (NumberFormatException|NullPointerException e) {
            throw new IllegalModelStructureException("Wrong status",
                    (node.get(STATUS_KEY) != null ? node.get(STATUS_KEY).asText() : "null"));
        }
    }

    private ConversionResponse parseDataBlock(JsonNode node) {
        if (node.get("data") != null) {
            var entryNodeIterator = node.get("data").fields();
            if (entryNodeIterator.hasNext()) {
                var entry = entryNodeIterator.next();
                var conversionResponse = new ConversionResponseX();
                conversionResponse.setRate(parseRate(entry.getValue()));
                String[] currencyPair = parseCurrencyPair(entry.getKey());
                conversionResponse.setFrom(Currency.getBy(currencyPair[0]));
                conversionResponse.setTo(Currency.getBy(currencyPair[1]));
                if (conversionResponse.getFrom() == null) {
                    throw new IllegalModelStructureException("Non registered currency code: ", currencyPair[0]);
                }
                if (conversionResponse.getTo() == null) {
                    throw new IllegalModelStructureException("Non registered currency code: ", currencyPair[1]);
                }
                return conversionResponse;

            }
        }
        throw new IllegalModelStructureException("Empty data", node.toString());
    }

    private String[] parseCurrencyPair(String strPair) {
        if (strPair.length() == 6) {
            String[] pair = new String[2];
            pair[0] = strPair.substring(0,3);
            pair[1] = strPair.substring(3);
            return pair;
        }
        throw new IllegalModelStructureException("Illegal currency pair", strPair);
    }

    private BigDecimal parseRate(JsonNode node) {
        if (node == null)
            throw new IllegalModelStructureException("Illegal rate", "null");

        try {
            return new BigDecimal(node.asText());
        } catch (NumberFormatException e) {
            throw new IllegalModelStructureException("Illegal rate", node.asText());
        }
    }
}
