package id.kromaklub;

final class ColorMatch {
    // Average the center of the photographed square, matching the viewfinder reticle.
    static int average(int[] pixels) {
        long red = 0;
        long green = 0;
        long blue = 0;
        for (int color : pixels) {
            red += (color >> 16) & 255;
            green += (color >> 8) & 255;
            blue += color & 255;
        }
        return 0xFF000000 | ((int) (red / pixels.length) << 16)
            | ((int) (green / pixels.length) << 8) | (int) (blue / pixels.length);
    }

    static int percent(int target, int sample) {
        double[] first = lab(target);
        double[] second = lab(sample);
        double l = first[0] - second[0];
        double a = first[1] - second[1];
        double b = first[2] - second[2];
        double distance = Math.sqrt(l * l + a * a + b * b);
        return Math.max(0, (int) Math.round(100 - distance));
    }

    static boolean light(int color) {
        double luminance = 0.2126 * linear((color >> 16) & 255)
            + 0.7152 * linear((color >> 8) & 255) + 0.0722 * linear(color & 255);
        return luminance > 0.18;
    }

    private static double[] lab(int color) {
        double r = linear((color >> 16) & 255);
        double g = linear((color >> 8) & 255);
        double b = linear(color & 255);
        double x = pivot((0.4124564 * r + 0.3575761 * g + 0.1804375 * b) / 0.95047);
        double y = pivot(0.2126729 * r + 0.7151522 * g + 0.0721750 * b);
        double z = pivot((0.0193339 * r + 0.1191920 * g + 0.9503041 * b) / 1.08883);
        return new double[] {116 * y - 16, 500 * (x - y), 200 * (y - z)};
    }

    private static double linear(int channel) {
        double value = channel / 255.0;
        return value <= 0.04045 ? value / 12.92 : Math.pow((value + 0.055) / 1.055, 2.4);
    }

    private static double pivot(double value) {
        return value > 0.008856 ? Math.cbrt(value) : 7.787037 * value + 16.0 / 116;
    }

    private ColorMatch() {}
}
