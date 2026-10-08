package com.eaglebank.model;

/**
 * Provides support for field error detail.
 *
 * @author mattbateup
 */
public record FieldErrorDetail(String field, String message, String type) {
}
