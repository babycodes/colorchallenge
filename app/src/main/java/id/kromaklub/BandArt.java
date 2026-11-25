package id.kromaklub;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

/** Screen-printed diagonal color bands behind the palette stickers. */
final class BandArt extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final Palettes.Swatch[] colors;

    BandArt(Context context) {
        super(context);
        colors = null;
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    BandArt(Context context, Palettes.Palette palette) {
        super(context);
        colors = palette.colors();
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    @Override protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight(), lean = w * .24f;
        if (colors == null) {
            c.save();
            c.translate(w * .78f, h * .49f);
            for (int i = 0; i < 12; i++) {
                paint.setColor(i % 2 == 0 ? 0xFF7748A8 : 0xFF5F4197);
                path.reset();
                path.moveTo(0, 0);
                path.lineTo(w * 1.5f, 0);
                path.lineTo(w * 1.5f, w * .31f);
                path.close();
                c.drawPath(path, paint);
                c.rotate(30f);
            }
            c.restore();
            return;
        }
        float band = (w + lean) / 5f;
        for (int i = 0; i < 5; i++) {
            float x = i * band - lean;
            path.reset();
            path.moveTo(x, 0);
            path.lineTo(x + band + 1, 0);
            path.lineTo(x + band + lean + 1, h);
            path.lineTo(x + lean, h);
            path.close();
            paint.setColor(colors[i].rgb());
            c.drawPath(path, paint);
        }
        float step = Math.max(w / 25f, 5f);
        paint.setColor(0x44FFFFFF);
        for (float x = step / 2; x < w; x += step) {
            for (float y = step / 2; y < h * .42f; y += step) {
                c.drawCircle(x, y, step * .19f * (1f - y / h), paint);
            }
        }
    }
}
