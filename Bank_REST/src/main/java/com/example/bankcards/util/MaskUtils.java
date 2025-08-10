package com.example.bankcards.util;

public final class MaskUtils {
    private MaskUtils() {}

    public static String maskPan(String pan) {
        if (pan == null || pan.length() < 4) return "****";
        String last4 = pan.substring(pan.length() - 4);
        return "**** **** **** " + last4;
    }
}
