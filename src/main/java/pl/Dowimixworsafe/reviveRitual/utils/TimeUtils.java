package pl.Dowimixworsafe.reviveRitual.utils;

import java.util.concurrent.TimeUnit;

public class TimeUtils {

    public static String formatTime(long millis) {
        if (millis < 0) {
            return "00:00:00";
        }

        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    public static long parseTimeString(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty())
            return -1;
        timeStr = timeStr.trim().toLowerCase();

        char lastChar = timeStr.charAt(timeStr.length() - 1);
        long multiplier = 60 * 1000L;

        String numberStr = timeStr;
        if (Character.isAlphabetic(lastChar)) {
            numberStr = timeStr.substring(0, timeStr.length() - 1);
            switch (lastChar) {
                case 's':
                    multiplier = 1000L;
                    break;
                case 'm':
                    multiplier = 60 * 1000L;
                    break;
                case 'h':
                    multiplier = 60 * 60 * 1000L;
                    break;
                case 'd':
                    multiplier = 24 * 60 * 60 * 1000L;
                    break;
                default:
                    return -1;
            }
        }

        try {
            long value = Long.parseLong(numberStr);
            if (value <= 0)
                return -1;
            return value * multiplier;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}