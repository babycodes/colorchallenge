package id.kromaklub;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ColorMatchTest {
    @Test public void exactMatchScoresOneHundred() {
        assertEquals(100, ColorMatch.percent(0xFFED354A, 0xFFED354A));
    }

    @Test public void nearbyColorOutranksDifferentHue() {
        int red = 0xFFED354A;
        assertTrue(ColorMatch.percent(red, 0xFFE83A48) > ColorMatch.percent(red, 0xFF3775F5));
        assertEquals(0, ColorMatch.percent(0xFF0000FF, 0xFFFFFF00));
    }

    @Test public void averageUsesAllChannels() {
        assertEquals(0xFF604020, ColorMatch.average(new int[] {0xFF804020, 0xFF404020}));
    }
}
