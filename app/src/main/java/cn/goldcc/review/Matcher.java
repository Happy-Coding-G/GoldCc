package cn.goldcc.review;

import java.util.ArrayList;
import java.util.List;

/** Input image has been resized proportionally to a height of 600 pixels. */
public final class Matcher {
    public static final class Hit {
        public final int slot, unit, error;
        Hit(int slot, int unit, int error) {
            this.slot = slot;
            this.unit = unit;
            this.error = error;
        }
    }
    private static int delta(int a, int b) {
        return Math.abs((a >> 16 & 255) - (b >> 16 & 255))
             + Math.abs((a >> 8 & 255) - (b >> 8 & 255))
             + Math.abs((a & 255) - (b & 255));
    }
    public static List<Hit> recognize(int[] pixels, int width, int row, List<int[]> portraits, List<String> names) {
        List<Hit> hits = new ArrayList<>();
        if (width < 950 || width > 1600 || row < 0 || row > 7) return hits;
        for (int slot = 0; slot < 10; slot++) {
            int left = Math.round(width * .5f - 248 + slot * 42.25f);
            int top = Math.round(97 + row * 52.8f);
            int low = 255, high = 0;
            for (int y = 9; y <= 28; y += 3) {
                for (int x = 5; x <= 31; x += 3) {
                    int rgb = pixels[(top + y) * width + left + x];
                    int light = ((rgb >> 16 & 255) + 2 * (rgb >> 8 & 255) + (rgb & 255)) / 4;
                    low = Math.min(low, light);
                    high = Math.max(high, light);
                }
            }
            if (high - low < 35) continue;
            int best = Integer.MAX_VALUE, runner = Integer.MAX_VALUE, winner = -1;
            for (int unit = 0; unit < portraits.size(); unit++) {
                int[] icon = portraits.get(unit);
                int min = Integer.MAX_VALUE;
                for (int dx = -2; dx <= 2; dx += 2) {
                    for (int dy = -2; dy <= 2; dy += 2) {
                        int sum = 0;
                        for (int y = 16; y < 51; y += 5) {
                            int offset = (top + dy + Math.round(y * 36f / 64)) * width + left + dx;
                            for (int x = 7; x < 58; x += 5) {
                                sum += delta(icon[y * 64 + x], pixels[offset + Math.round(x * 36f / 64)]);
                            }
                        }
                        min = Math.min(min, sum / (7 * 11 * 3));
                    }
                }
                if (min < best) {
                    if (winner < 0 || !names.get(winner).equals(names.get(unit))) runner = best;
                    best = min;
                    winner = unit;
                } else if (!names.get(winner).equals(names.get(unit))) runner = Math.min(min, runner);
            }
            if (best < 37 && runner - best >= 5) hits.add(new Hit(slot, winner, best));
        }
        return hits;
    }
}
