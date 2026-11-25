package id.kromaklub;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

final class StickerView extends View {
    enum Art { PRISM, SPECTRUM, COAST, MARKET, CLOUD, TEAM, TARGET, TROPHY }

    private static final int INK = 0xFF21173A;
    private static final int WHITE = 0xFFFFF7FC;
    private static final int LIME = 0xFFDCFA79;
    private static final int GOLD = 0xFFFFCF62;
    private static final int PINK = 0xFFFF8BB6;
    private static final int BLUE = 0xFF75DEFA;
    private static final int LILAC = 0xFFBBA6FF;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private Art art;

    StickerView(Context context) {
        this(context, Art.PRISM);
    }

    StickerView(Context context, Art art) {
        super(context);
        this.art = art;
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    void setArt(Art art) {
        this.art = art;
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float unit = Math.min(getWidth() / 128f, getHeight() / 120f);
        canvas.save();
        canvas.translate((getWidth() - 128 * unit) / 2, (getHeight() - 120 * unit) / 2);
        canvas.scale(unit, unit);
        switch (art) {
            case PRISM -> prism(canvas);
            case SPECTRUM -> spectrum(canvas);
            case COAST -> coast(canvas);
            case MARKET -> market(canvas);
            case CLOUD -> cloud(canvas);
            case TEAM -> team(canvas);
            case TARGET -> target(canvas);
            case TROPHY -> trophy(canvas);
        }
        spark(canvas, 108, 18, 9, LIME);
        spark(canvas, 15, 92, 8, PINK);
        circle(canvas, 106, 89, 4, WHITE);
        canvas.restore();
    }

    private void prism(Canvas c) {
        shape(c, PINK, 22, 69, 14, 46, 34, 50);
        shape(c, BLUE, 89, 41, 108, 34, 111, 56);
        polygon(32, 35, 61, 16, 95, 33, 101, 77, 69, 101, 33, 81);
        stroke(c, WHITE, 11);
        shape(c, LILAC, 32, 35, 61, 16, 95, 33, 101, 77, 69, 101, 33, 81);
        shape(c, GOLD, 32, 35, 61, 16, 95, 33, 66, 54);
        shape(c, LIME, 32, 35, 66, 54, 69, 101, 33, 81);
        line(c, 44, 40, 58, 47, WHITE, 3);
        face(c, 47, 63);
        spark(c, 79, 65, 8, WHITE);
    }

    private void spectrum(Canvas c) {
        outlinedCircle(c, 62, 57, 39, BLUE);
        outlinedCircle(c, 62, 57, 29, PINK);
        outlinedCircle(c, 62, 57, 19, GOLD);
        spark(c, 63, 56, 11, WHITE);
        circle(c, 29, 50, 3, WHITE);
        circle(c, 84, 83, 4, WHITE);
        shape(c, LIME, 23, 80, 30, 72, 36, 80, 30, 89);
    }

    private void coast(Canvas c) {
        outlinedCircle(c, 73, 45, 27, GOLD);
        path.reset();
        path.moveTo(21, 83);
        path.cubicTo(42, 91, 47, 36, 74, 53);
        path.cubicTo(66, 56, 65, 70, 76, 73);
        path.cubicTo(91, 79, 100, 59, 108, 72);
        path.lineTo(110, 88);
        path.cubicTo(81, 109, 42, 106, 21, 83);
        path.close();
        fill(c, BLUE);
        stroke(c, INK, 4);
        line(c, 42, 88, 57, 82, WHITE, 4);
        line(c, 77, 90, 93, 85, WHITE, 3);
    }

    private void market(Canvas c) {
        line(c, 22, 27, 106, 34, INK, 4);
        lantern(c, 33, 51, PINK);
        lantern(c, 61, 63, GOLD);
        lantern(c, 91, 54, LIME);
    }

    private void lantern(Canvas c, float x, float y, int color) {
        line(c, x, 29, x, y - 13, INK, 3);
        line(c, x, y + 12, x, y + 25, INK, 3);
        outlinedCircle(c, x, y, 15, color);
        line(c, x - 5, y - 4, x - 5, y + 4, WHITE, 3);
    }

    private void cloud(Canvas c) {
        path.reset();
        path.moveTo(31, 83);
        path.cubicTo(14, 79, 18, 56, 36, 54);
        path.cubicTo(36, 31, 71, 25, 80, 49);
        path.cubicTo(103, 43, 116, 69, 100, 82);
        path.cubicTo(92, 89, 51, 88, 31, 83);
        path.close();
        fill(c, PINK);
        stroke(c, INK, 4);
        face(c, 62, 62);
        line(c, 36, 73, 46, 75, WHITE, 3);
    }

    private void team(Canvas c) {
        outlinedCircle(c, 43, 57, 25, BLUE);
        outlinedCircle(c, 82, 60, 26, GOLD);
        face(c, 42, 53);
        face(c, 82, 57);
    }

    private void target(Canvas c) {
        outlinedCircle(c, 64, 58, 38, WHITE);
        outlinedCircle(c, 64, 58, 28, PINK);
        outlinedCircle(c, 64, 58, 17, GOLD);
        spark(c, 64, 58, 11, WHITE);
    }

    private void trophy(Canvas c) {
        shape(c, GOLD, 50, 74, 78, 74, 80, 84, 91, 92, 91, 100, 37, 100, 37, 92, 48, 84);
        outlinedCircle(c, 32, 49, 14, PINK);
        outlinedCircle(c, 96, 49, 14, PINK);
        path.reset();
        path.moveTo(36, 28);
        path.lineTo(92, 28);
        path.lineTo(88, 57);
        path.cubicTo(84, 82, 44, 82, 40, 57);
        path.close();
        fill(c, GOLD);
        stroke(c, INK, 4);
        spark(c, 64, 49, 13, WHITE);
    }

    private void face(Canvas c, float x, float y) {
        line(c, x - 8, y - 2, x - 8, y + 3, INK, 3.5f);
        line(c, x + 8, y - 2, x + 8, y + 3, INK, 3.5f);
        path.reset();
        path.moveTo(x - 4, y + 7);
        path.quadTo(x, y + 12, x + 4, y + 7);
        stroke(c, INK, 3);
    }

    private void spark(Canvas c, float x, float y, float size, int color) {
        shape(c, color, x, y - size, x + 3, y - 3, x + size, y, x + 3, y + 3,
            x, y + size, x - 3, y + 3, x - size, y, x - 3, y - 3);
    }

    private void outlinedCircle(Canvas c, float x, float y, float radius, int color) {
        circle(c, x, y, radius + 2, INK);
        circle(c, x, y, radius - 2, color);
    }

    private void circle(Canvas c, float x, float y, float radius, int color) {
        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);
        c.drawCircle(x, y, radius, paint);
    }

    private void line(Canvas c, float x, float y, float toX, float toY, int color, float width) {
        path.reset();
        path.moveTo(x, y);
        path.lineTo(toX, toY);
        stroke(c, color, width);
    }

    private void shape(Canvas c, int color, float... points) {
        polygon(points);
        fill(c, color);
        stroke(c, INK, 3);
    }

    private void polygon(float... points) {
        path.reset();
        path.moveTo(points[0], points[1]);
        for (int i = 2; i < points.length; i += 2) path.lineTo(points[i], points[i + 1]);
        path.close();
    }

    private void fill(Canvas c, int color) {
        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);
        c.drawPath(path, paint);
    }

    private void stroke(Canvas c, int color, float width) {
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(width);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        c.drawPath(path, paint);
    }
}
