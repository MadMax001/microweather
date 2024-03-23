package ru.madmax.pet.microcurrency.currate.misc;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import ru.madmax.pet.microcurrency.currate.exception.IllegalModelStructureException;
import ru.madmax.pet.microcurrency.currate.model.RemoteResponse;
import ru.madmax.pet.microcurrency.common.model.Currency;

import java.io.IOException;
import java.math.BigDecimal;

public class RemoteConversionDeserializer extends StdDeserializer<RemoteResponse> {
    private static final String STATUS_KEY = "status";
    public RemoteConversionDeserializer() {
        this(null);
    }

    public RemoteConversionDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public RemoteResponse deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
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
                    (node.get(STATUS_KEY) != null ? node.get(STATUS_KEY).asText() : node.toString()));
        }
    }

    private RemoteResponse parseDataBlock(JsonNode node) {
        if (node.get("data") != null) {
            var entryNodeIterator = node.get("data").fields();
            if (entryNodeIterator.hasNext()) {
                var entry = entryNodeIterator.next();
                var response = new RemoteResponse();
                response.setRate(parseRate(entry.getValue()));
                String[] currencyPair = parseCurrencyPair(entry.getKey());
                response.setFrom(Currency.getBy(currencyPair[0]));
                response.setTo(Currency.getBy(currencyPair[1]));
                if (response.getFrom() == null) {
                    throw new IllegalModelStructureException("Non registered currency code: ", currencyPair[0]);
                }
                if (response.getTo() == null) {
                    throw new IllegalModelStructureException("Non registered currency code: ", currencyPair[1]);
                }
                return response;

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
