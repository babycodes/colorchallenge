package id.kromaklub;

import org.junit.Test;
import static org.junit.Assert.*;

public class HuntClockTest {
    @Test public void revealingTargetAloneDoesNotUnlockCamera() {
        assertFalse(HuntClock.cameraAvailable(0, 30, 9000000));
    }

    @Test public void cameraGateUsesElapsedTimeForEveryDuration() {
        long start = 100000;
        for (int seconds = 20; seconds <= 120; seconds += 5) {
            long end = start + seconds * 1000L;
            assertFalse(HuntClock.cameraAvailable(end, seconds, start + 19999));
            assertTrue(HuntClock.cameraAvailable(end, seconds, start + 20000));
            assertTrue(HuntClock.cameraAvailable(end, seconds, end + 1000));
            assertEquals(0, HuntClock.remaining(end, end + 1000));
        }
    }
}
