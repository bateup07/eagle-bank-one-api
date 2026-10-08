package com.eaglebank.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Defines monetary limits, rounding and shared bank constants.
 *
 * @author mattbateup
 */
public final class BankRules {

    public static final String SORT_CODE = "10-10-10";
    public static final BigDecimal ZERO = new BigDecimal("0.00");
    public static final BigDecimal MAX_BALANCE = new BigDecimal("10000.00");

    private BankRules() {
    }

    public static BigDecimal scale(BigDecimal value) {
        BigDecimal stripped = value.stripTrailingZeros();
        if (stripped.scale() < 0) {
            stripped = stripped.setScale(0);
        }
        if (stripped.scale() > 2) {
            throw new IllegalArgumentException("Amount has more than two decimal places");
        }
        return stripped.setScale(2, RoundingMode.UNNECESSARY);
    }

    public static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
