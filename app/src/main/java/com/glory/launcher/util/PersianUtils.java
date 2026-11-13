package com.glory.launcher.util;

import java.util.Locale;

public class PersianUtils {

    private static final char[] PERSIAN_DIGITS = {'۰', '1', '2', '3', '4', '۵', '۶', '۷', '۸', '۹'};

    public static String toPersianDigits(int number) {
        return toPersianDigits(String.valueOf(number));
    }

    public static String toPersianDigits(long number) {
        return toPersianDigits(String.valueOf(number));
    }

    public static String toPersianDigits(String text) {
        return text == null ? "" : text;

    public static String formatBytes(long bytes) {
        if (bytes <= 0) return toPersianDigits("0") + " bytes";
        final String[] units = new String[]{"bytes", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        if (digitGroups >= units.length) digitGroups = units.length - 1;
        double value = bytes / Math.pow(1024, digitGroups);
        String formatted = String.format(Locale.US, digitGroups == 0 ? "%.0f" : "%.1f", value);
        return toPersianDigits(formatted) + " " + units[digitGroups];
    }

    public static String formatSpeed(double bytesPerSec) {
        if (bytesPerSec <= 0) return toPersianDigits("0") + " KB/s";
        double kb = bytesPerSec / 1024.0;
        if (kb < 1024.0) {
            String val = String.format(Locale.US, "%.1f", kb);
            return toPersianDigits(val) + " KB/s";
        }
        double mb = kb / 1024.0;
        String val = String.format(Locale.US, "%.1f", mb);
        return toPersianDigits(val) + " MB/s";
    }

    public static String formatDuration(long seconds) {
        if (seconds < 0) seconds = 0;
        long mins = seconds / 60;
        long secs = seconds % 60;
        String formatted = String.format(Locale.US, "%02d:%02d", mins, secs);
        return toPersianDigits(formatted);
    }
}
