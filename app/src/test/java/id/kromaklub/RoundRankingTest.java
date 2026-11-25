package id.kromaklub;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RoundRankingTest {
    @Test(expected = IllegalStateException.class)
    public void incompleteRoundCannotRevealScores() {
        RoundRanking.reveal(0xFFFA0000, 3, new int[] {0xFFFA0000, 0xFFE50000});
    }

    @Test public void revealsSortedScoresAndKeepsTiedPlayersInTurnOrder() {
        int target = 0xFFFA0000;
        var result = RoundRanking.reveal(target, 4, new int[] {0xFF003AFF, target, 0xFFE50000, target});
        assertEquals(1, result.get(0).player());
        assertEquals(100, result.get(0).score());
        assertEquals(3, result.get(1).player());
        assertEquals(100, result.get(1).score());
        assertEquals(2, result.get(2).player());
        assertEquals(0, result.get(3).player());
    }
}
