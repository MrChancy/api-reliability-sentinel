package com.fluffycat.sentinelapp.common.util;

import java.util.regex.Pattern;

public class EmailUtils {
    // 相对完善的正则：支持字母、数字、点、下划线、加号，以及 2-64 位的域名后缀
    private static final String EMAIL_REGEX = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,64}$";
    private static final Pattern PATTERN = Pattern.compile(EMAIL_REGEX);

    public static boolean isEmail(String email) {
        if (email == null || email.isBlank()) return false;
        return PATTERN.matcher(email).matches();
    }
}
