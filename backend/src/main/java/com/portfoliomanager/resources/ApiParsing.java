package com.portfoliomanager.resources;

import java.math.BigDecimal;
import java.util.UUID;

final class ApiParsing {
    private ApiParsing() {}
    static UUID id(String text) {
        try {
            UUID id = UUID.fromString(text);
            if (!id.toString().equalsIgnoreCase(text)) throw new IllegalArgumentException();
            return id;
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("ID must be a UUID");
        }
    }
    static BigDecimal money(String text) {
        if (text == null) return null;
        try { return new BigDecimal(text); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Amount must be a decimal string"); }
    }
}
