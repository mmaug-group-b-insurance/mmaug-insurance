package com.mmaug.claimsagent.common.util;

public final class PiiMasker {

    private PiiMasker() {}

    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        String[] parts = email.split("@", 2);
        String local = parts[0];
        if (local.length() <= 2) return "***@" + parts[1];
        return local.charAt(0) + "***" + local.charAt(local.length() - 1) + "@" + parts[1];
    }

    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return phone;
        return "***" + phone.substring(phone.length() - 4);
    }

    public static String maskName(String name) {
        if (name == null || name.isBlank()) return name;
        return name.charAt(0) + "***";
    }
}