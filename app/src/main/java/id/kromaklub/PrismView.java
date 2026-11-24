package id.kromaklub;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

final class PrismView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();

    PrismView(Context context) {
        super(context);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float unit = Math.min(getWidth() / 124f, getHeight() / 120f);
        canvas.save();
        canvas.translate((getWidth() - 124 * unit) / 2, (getHeight() - 120 * unit) / 2);
        canvas.scale(unit, unit);
        polygon(canvas, 0xFF7463C7, 63, 2, 115, 29, 115, 83, 63, 111, 11, 83, 11, 29);
        polygon(canvas, 0xFFFFA584, 63, 13, 105, 35, 63, 58, 21, 35);
        polygon(canvas, 0xFFBBA8FF, 105, 35, 105, 78, 63, 101, 63, 58);
        polygon(canvas, 0xFFE6FC81, 21, 35, 63, 58, 63, 101, 21, 78);
        polygon(canvas, 0xFF15132A, 30, 51, 36, 54, 36, 64, 30, 61);
        polygon(canvas, 0xFF15132A, 49, 61, 55, 64, 55, 74, 49, 71);
        polygon(canvas, 0xFF15132A, 37, 70, 47, 76, 47, 80, 37, 74);
        polygon(canvas, 0xFFE6FC81, 114, 0, 117, 8, 124, 11, 117, 14, 114, 22, 111, 14, 104, 11, 111, 8);
        polygon(canvas, 0xFFFFA584, 12, 89, 15, 98, 24, 101, 15, 104, 12, 113, 9, 104, 0, 101, 9, 98);
        canvas.restore();
    }

    private void polygon(Canvas canvas, int color, float... points) {
        path.reset();
        path.moveTo(points[0], points[1]);
        for (int i = 2; i < points.length; i += 2) path.lineTo(points[i], points[i + 1]);
        path.close();
        paint.setColor(color);
        canvas.drawPath(path, paint);
    }
}
