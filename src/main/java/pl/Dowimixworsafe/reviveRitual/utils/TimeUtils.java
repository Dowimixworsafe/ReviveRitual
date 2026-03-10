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
}