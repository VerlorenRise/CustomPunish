/*
 * Decompiled with CFR 0.152.
 */
package ru.projectn4.punish.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TimeUtil {
    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d+)([smhdwMy])");
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());

    private TimeUtil() {
    }

    public static long parseDuration(String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("Строка времени пуста");
        }
        String normalized = input.trim().toLowerCase(Locale.ROOT);
        if (normalized.equals("perm") || normalized.equals("permanent") || normalized.equals("навсегда") || normalized.equals("бессрочно") || normalized.equals("infinity") || normalized.equals("-1")) {
            return -1L;
        }
        Matcher matcher = TIME_PATTERN.matcher(normalized);
        long totalMillis = 0L;
        boolean found = false;
        block18: while (matcher.find()) {
            String unit;
            found = true;
            long value = Long.parseLong(matcher.group(1));
            switch (unit = matcher.group(2)) {
                case "s": {
                    totalMillis += value * 1000L;
                    continue block18;
                }
                case "m": {
                    totalMillis += value * 60L * 1000L;
                    continue block18;
                }
                case "h": {
                    totalMillis += value * 60L * 60L * 1000L;
                    continue block18;
                }
                case "d": {
                    totalMillis += value * 24L * 60L * 60L * 1000L;
                    continue block18;
                }
                case "w": {
                    totalMillis += value * 7L * 24L * 60L * 60L * 1000L;
                    continue block18;
                }
                case "M": {
                    totalMillis += value * 30L * 24L * 60L * 60L * 1000L;
                    continue block18;
                }
                case "y": {
                    totalMillis += value * 365L * 24L * 60L * 60L * 1000L;
                    continue block18;
                }
            }
            throw new IllegalArgumentException("Неизвестная единица измерения времени: " + unit);
        }
        if (!found) {
            throw new IllegalArgumentException("Неверный формат времени: " + input);
        }
        return totalMillis;
    }

    public static boolean isValidDuration(String input) {
        try {
            TimeUtil.parseDuration(input);
            return true;
        }
        catch (Exception e) {
            return false;
        }
    }

    public static String formatDuration(long millis) {
        String result;
        if (millis <= 0L) {
            return "Навсегда";
        }
        long seconds = millis / 1000L;
        long minutes = seconds / 60L;
        long hours = minutes / 60L;
        long days = hours / 24L;
        StringBuilder sb = new StringBuilder();
        if (days > 0L) {
            sb.append(days).append(" дн. ");
            hours %= 24L;
            minutes %= 60L;
            seconds %= 60L;
        }
        if (hours > 0L) {
            sb.append(hours).append(" ч. ");
            minutes %= 60L;
            seconds %= 60L;
        }
        if (minutes > 0L) {
            sb.append(minutes).append(" мин. ");
            seconds %= 60L;
        }
        if (days == 0L && hours == 0L && seconds > 0L) {
            sb.append(seconds).append(" сек.");
        }
        return (result = sb.toString().trim()).isEmpty() ? "менее секунды" : result;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static String formatDate(long timestamp) {
        if (timestamp <= 0L) {
            return "Никогда";
        }
        SimpleDateFormat simpleDateFormat = DATE_FORMAT;
        synchronized (simpleDateFormat) {
            return DATE_FORMAT.format(new Date(timestamp));
        }
    }
}

