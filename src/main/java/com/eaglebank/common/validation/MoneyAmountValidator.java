package com.eaglebank.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.math.BigDecimal;

/**
 * Provides custom input validation.
 *
 * @author mattbateup
 */
public class MoneyAmountValidator implements ConstraintValidator<MoneyAmount, BigDecimal> {

    private static final BigDecimal MAX = new BigDecimal("10000.00");

    @Override
    public boolean isValid(BigDecimal value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(MAX) > 0) {
            return false;
        }
        BigDecimal stripped = value.stripTrailingZeros();
        int scale = Math.max(stripped.scale(), 0);
        return scale <= 2;
    }
}
