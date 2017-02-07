package common;

import java.time.Duration;

public class DurationKit {
    private DurationKit() {
    }

    public static Duration parse(String durationInIso8601) {
        return Duration.parse(durationInIso8601);
    }

    public static String inHours(Duration duration) {
        return duration.toHours() + " hour(s)";
    }
}
