package com.eaglebank.common;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/**
 * Generates identifiers matching the API user, account and transaction patterns.
 *
 * @author mattbateup
 */
@Component
public class IdGenerator {

    private static final char[] ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toCharArray();

    private final SecureRandom random = new SecureRandom();

    public String userId() {
        return "usr-" + randomToken(12);
    }

    public String transactionId() {
        return "tan-" + randomToken(12);
    }

    public String accountNumber() {
        return "01" + String.format("%06d", random.nextInt(1_000_000));
    }

    private String randomToken(int length) {
        char[] chars = new char[length];
        for (int i = 0; i < length; i++) {
            chars[i] = ALPHABET[random.nextInt(ALPHABET.length)];
        }
        return new String(chars);
    }
}
