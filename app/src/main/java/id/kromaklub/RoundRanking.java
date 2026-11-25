package id.kromaklub;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class RoundRanking {
    record Entry(int player, int score) {}

    static List<Entry> reveal(int target, int playerCount, int[] capturedColors) {
        if (playerCount < 2 || playerCount > 4 || capturedColors.length != playerCount) {
            throw new IllegalStateException("A complete round is required to reveal scores");
        }
        List<Entry> entries = new ArrayList<>(playerCount);
        for (int index = 0; index < playerCount; index++) {
            entries.add(new Entry(index, ColorMatch.percent(target, capturedColors[index])));
        }
        Collections.sort(entries, (first, second) -> Integer.compare(second.score(), first.score()));
        return entries;
    }

    private RoundRanking() {}
}
