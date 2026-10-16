package za.co.fraud.engine.domain;

/** Masks account identifiers before they appear in rule reasons, responses or logs (POPIA data minimisation). */
public final class Masking {

    private Masking() {
    }

    public static String account(String account) {
        if (account == null || account.length() <= 4) {
            return "****";
        }
        return "****" + account.substring(account.length() - 4);
    }
}
