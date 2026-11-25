package id.kromaklub;

/** Monotonic time keeps the same camera gate across Activity recreation. */
final class HuntClock {
    static final int UNLOCK_SECONDS = 20;

    static long remaining(long deadline, long now) {
        return Math.max(0, deadline - now);
    }

    static boolean cameraAvailable(long deadline, int durationSeconds, long now) {
        long startedAt = deadline - durationSeconds * 1000L;
        return deadline > 0 && now - startedAt >= UNLOCK_SECONDS * 1000L;
    }

    private HuntClock() {}
}
