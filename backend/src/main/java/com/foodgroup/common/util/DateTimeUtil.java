package com.foodgroup.common.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public final class DateTimeUtil {

    private DateTimeUtil() {}

    public static LocalDateTime parse(String s) {
        if (s == null) return null;
        return s.endsWith("Z")
                ? Instant.parse(s).atZone(ZoneOffset.UTC).toLocalDateTime()
                : LocalDateTime.parse(s);
    }
}
