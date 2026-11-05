package com.fitos.order;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Sinh mã đơn dạng ORD-20261025-7K3QZP (UNIQUE ở DB, service retry khi trùng). */
@Component
public class OrderReferenceGenerator {

    private static final char[] ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final DateTimeFormatter DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final SecureRandom random = new SecureRandom();
    private final Clock clock;

    public OrderReferenceGenerator(Clock clock) {
        this.clock = clock;
    }

    public String next() {
        StringBuilder sb = new StringBuilder("ORD-").append(LocalDate.now(clock).format(DATE)).append('-');
        for (int i = 0; i < 6; i++) {
            sb.append(ALPHABET[random.nextInt(ALPHABET.length)]);
        }
        return sb.toString();
    }
}
