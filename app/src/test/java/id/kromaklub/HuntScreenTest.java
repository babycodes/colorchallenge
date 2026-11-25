package id.kromaklub;

import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.time.Duration;
import java.util.ArrayList;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class HuntScreenTest {
    @Test public void targetWaitsForStartAndCameraUnlockSurvivesRecreation() {
        var screen = Robolectric.buildActivity(MainActivity.class).setup();
        try {
            control(screen.get(), "Mulai misi  →").performClick();
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMinutes(1));
            assertNull(control(screen.get(), "Buka kamera"));
            assertNull(control(screen.get(), "00:30"));
            screen.recreate();
            control(screen.get(), "Mulai mencari").performClick();
            assertNotNull(control(screen.get(), "00:30"));
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(19));
            TextView locked = control(screen.get(), "Buka kamera");
            assertTrue(locked == null || !locked.isShown());
            screen.recreate();
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1100));
            assertTrue(control(screen.get(), "Buka kamera").isShown());
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(11));
            assertNotNull(control(screen.get(), "00:00"));
            assertTrue(control(screen.get(), "Buka kamera").isShown());
            control(screen.get(), "Buka kamera").performClick();
            assertNotNull(control(screen.get(), "Izinkan kamera"));
        } finally {
            screen.pause().stop().destroy();
        }
    }

    @Test public void rosterStartsWithATargetFromTheSelectedPalette() {
        var screen = Robolectric.buildActivity(MainActivity.class).setup();
        try {
            control(screen.get(), "›").performClick();
            control(screen.get(), "2 pemain ＋").performClick();
            control(screen.get(), "Mulai misi  →").performClick();
            assertNotNull(control(screen.get(), "Pesisir"));
            boolean found = false;
            for (var swatch : Palettes.ALL[1].colors()) {
                if (control(screen.get(), swatch.name()) != null) found = true;
            }
            assertTrue(found);
            assertNotNull(control(screen.get(), "Mulai mencari"));
            assertNull(control(screen.get(), "00:30"));
        } finally {
            screen.pause().stop().destroy();
        }
    }

    private TextView control(MainActivity activity, String label) {
        ArrayList<View> matches = new ArrayList<>();
        activity.getWindow().getDecorView().findViewsWithText(matches, label, View.FIND_VIEWS_WITH_TEXT);
        for (View match : matches) {
            if (match instanceof TextView text && label.contentEquals(text.getText())) return text;
        }
        return null;
    }
}
