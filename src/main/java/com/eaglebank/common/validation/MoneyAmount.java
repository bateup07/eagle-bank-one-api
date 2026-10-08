package com.eaglebank.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Provides support for money amount.
 *
 * @author mattbateup
 */
@Documented
@Constraint(validatedBy = MoneyAmountValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface MoneyAmount {

    String message() default "must be between 0.00 and 10000.00 with at most two decimal places";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
