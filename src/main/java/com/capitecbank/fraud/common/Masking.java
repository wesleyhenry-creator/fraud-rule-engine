package com.capitecbank.fraud.common;

/** Masks account identifiers for output. Only the last four characters remain visible. */
public final class Masking {

    private static final int VISIBLE_SUFFIX = 4;

    private Masking() {
    }

    public static String mask(String value) {
        if (value == null) {
            return null;
        }
        if (value.length() <= VISIBLE_SUFFIX) {
            return "****";
        }
        return "*".repeat(value.length() - VISIBLE_SUFFIX)
                + value.substring(value.length() - VISIBLE_SUFFIX);
    }
}
