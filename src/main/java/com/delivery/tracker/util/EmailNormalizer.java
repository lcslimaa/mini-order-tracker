package com.delivery.tracker.util;

import java.util.Locale;

public final class EmailNormalizer {
    private EmailNormalizer() {}

    public static String normalize(String email) {
        return email == null ? "" : email.strip().toLowerCase(Locale.ROOT);
    }
}
