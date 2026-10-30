package com.portfoliomanager.common;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public final class LedgerValidation {
    private LedgerValidation() {}
    public static String name(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Name must not be blank");
        String value = name.strip();
        if (value.codePointCount(0, value.length()) > 255) throw new IllegalArgumentException("Name exceeds 255 characters");
        return value;
    }
    public static BigDecimal money(BigDecimal value) {
        if (value == null) throw new IllegalArgumentException("Amount is required");
        BigDecimal fixed;
        try { fixed = value.setScale(2, RoundingMode.UNNECESSARY); }
        catch (ArithmeticException e) { throw new IllegalArgumentException("Amount must have at most two decimal places", e); }
        if (fixed.precision() > 19) throw new IllegalArgumentException("Amount exceeds decimal(19,2)");
        return fixed;
    }
    public static Instant now() { return Instant.now().truncatedTo(ChronoUnit.MICROS); }
}
