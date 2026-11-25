package id.kromaklub;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.camera.view.PreviewView;
import androidx.core.content.FileProvider;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public final class MainActivity extends ComponentActivity {
    private static final int INK = 0xFF15132A;
    private static final int PANEL = 0xFF24213D;
    private static final int LINE = 0xFF484260;
    private static final int PAPER = 0xFFF8F2FF;
    private static final int MUTED = 0xFFC0BAD9;
    private static final int LIME = 0xFFE6FC81;
    private static final int LILAC = 0xFFBBA8FF;
    private static final int PEACH = 0xFFFFA584;
    private static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    private static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;
    private static final StickerView.Art[] PALETTE_ART = {
        StickerView.Art.SPECTRUM, StickerView.Art.COAST, StickerView.Art.MARKET, StickerView.Art.CLOUD
    };
    private static final int[] PALETTE_POSTER = {0xFFD5CCFF, 0xFFA9EDF5, 0xFFFFD18B, 0xFFFFC8DD};

    private enum Page { HOME, PLAYERS, HUNT, CAMERA, REVIEW, RESULTS }
    private record Shot(String file, int color) {}

    private final ArrayList<String> players = new ArrayList<>();
    private final ArrayList<Shot> shots = new ArrayList<>();
    private final Random random = new Random();
    private final Handler clock = new Handler(Looper.getMainLooper());
    private final Runnable tick = this::updateClock;

    private Page page = Page.HOME;
    private int paletteIndex;
    private int swatchIndex;
    private int seconds = 30;
    private int currentPlayer;
    private long deadline;
    private PhotoStore photos;
    private LiveCamera camera;
    private FrameLayout canvas;
    private LinearLayout shell;
    private LinearLayout body;
    private ScrollView scroll;
    private TextView timerLabel;
    private View huntProgress;
    private int backdrop;
    private boolean darkSystemIcons;
    private ValueAnimator cameraTint;

    private final ActivityResultLauncher<String> permission =
        registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            if (page != Page.CAMERA) return;
            if (granted) render();
            else showCameraAccess(true);
        });

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        photos = new PhotoStore(this);
        players.add("");
        players.add("");
        if (state != null) restore(state);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (page == Page.HOME) finish();
                else if (page == Page.PLAYERS) show(Page.HOME);
                else if (page == Page.RESULTS) returnToLobby();
                else confirmExit();
            }
        });
        render();
    }

    private void restore(Bundle state) {
        page = Page.valueOf(state.getString("page", Page.HOME.name()));
        paletteIndex = state.getInt("palette");
        swatchIndex = state.getInt("swatch");
        seconds = state.getInt("seconds", 30);
        currentPlayer = state.getInt("player");
        deadline = state.getLong("deadline");
        ArrayList<String> names = state.getStringArrayList("names");
        if (names != null) {
            players.clear();
            players.addAll(names);
        }
        ArrayList<String> files = state.getStringArrayList("files");
        int[] colors = state.getIntArray("colors");
        if (files != null && colors != null) {
            for (int i = 0; i < files.size(); i++) {
                shots.add(new Shot(files.get(i), colors[i]));
            }
        }
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putString("page", page.name());
        state.putInt("palette", paletteIndex);
        state.putInt("swatch", swatchIndex);
        state.putInt("seconds", seconds);
        state.putInt("player", currentPlayer);
        state.putLong("deadline", deadline);
        state.putStringArrayList("names", new ArrayList<>(players));
        ArrayList<String> files = new ArrayList<>();
        int[] colors = new int[shots.size()];
        for (int i = 0; i < shots.size(); i++) {
            files.add(shots.get(i).file());
            colors[i] = shots.get(i).color();
        }
        state.putStringArrayList("files", files);
        state.putIntArray("colors", colors);
    }

    @Override protected void onDestroy() {
        stopCamera();
        clock.removeCallbacks(tick);
        if (isFinishing()) discardShots();
        super.onDestroy();
    }

    private void show(Page next) {
        getSystemService(InputMethodManager.class).hideSoftInputFromWindow(
            getWindow().getDecorView().getWindowToken(), 0);
        page = next;
        render();
    }

    private void render() {
        stopCamera();
        clock.removeCallbacks(tick);
        canvas = new FrameLayout(this);
        setBackdrop(INK);
        shell = column();
        shell.setClipChildren(false);
        canvas.addView(shell, new FrameLayout.LayoutParams(MATCH, MATCH));
        ViewCompat.setOnApplyWindowInsetsListener(shell, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                | WindowInsetsCompat.Type.displayCutout() | WindowInsetsCompat.Type.ime());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        setContentView(canvas);
        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        shell.addView(scroll, new LinearLayout.LayoutParams(MATCH, 0, 1));
        body = column();
        body.setClipChildren(false);
        body.setFocusableInTouchMode(true);
        pad(body, 20, 14, 20, 18);
        scroll.addView(body, new ScrollView.LayoutParams(MATCH, WRAP));
        body.requestFocus();

        switch (page) {
            case HOME -> lobby();
            case PLAYERS -> roster();
            case HUNT -> hunt();
            case CAMERA -> {
                if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    viewfinder();
                } else {
                    showCameraAccess(false);
                }
            }
            case REVIEW -> review();
            case RESULTS -> results();
        }
        for (int i = 0; i < body.getChildCount(); i++) {
            enter(body.getChildAt(i), Math.min(i * 35L, 160L));
        }
    }

    private void lobby() {
        header("KROMA KLUB", players.size() + " pemain ＋", () -> show(Page.PLAYERS));
        LinearLayout hero = row();
        hero.setGravity(Gravity.CENTER_VERTICAL);
        GradientDrawable heroFill = new GradientDrawable(GradientDrawable.Orientation.BL_TR,
            new int[] {0xFF382D73, 0xFF6743AA});
        heroFill.setCornerRadius(dp(26));
        hero.setBackground(heroFill);
        pad(hero, 17, 12, 10, 12);
        LinearLayout lead = column();
        lead.addView(display("Misi Warna", 37, PAPER));
        TextView intro = text("Temukan warna target di sekitarmu.", 14, PAPER, false);
        pad(intro, 0, 8, 0, 0);
        lead.addView(intro);
        hero.addView(lead, new LinearLayout.LayoutParams(0, WRAP, 1));
        StickerView mascot = new StickerView(this);
        hero.addView(mascot, new LinearLayout.LayoutParams(dp(112), dp(100)));
        block(body, hero, 16);
        pop(mascot);

        TextView choose = text("Pilih palet", 19, PAPER, true);
        block(body, choose, 16);
        LinearLayout poster = row();
        poster.setGravity(Gravity.CENTER_VERTICAL);
        pad(poster, 12, 8, 15, 8);
        StickerView paletteArt = new StickerView(this, PALETTE_ART[paletteIndex]);
        poster.addView(paletteArt, new LinearLayout.LayoutParams(dp(106), dp(98)));
        LinearLayout paletteInfo = column();
        pad(paletteInfo, 6, 0, 0, 0);
        TextView paletteTitle = display(Palettes.ALL[paletteIndex].name(), 27, INK);
        paletteInfo.addView(paletteTitle);
        TextView description = text(Palettes.ALL[paletteIndex].description(), 13, INK, false);
        block(paletteInfo, description, 4);
        poster.addView(paletteInfo, new LinearLayout.LayoutParams(0, WRAP, 1));
        block(body, poster, 8);
        poster.setBackground(background(PALETTE_POSTER[paletteIndex], 21, 0));
        pop(paletteArt);

        ArrayList<LinearLayout> cards = new ArrayList<>();
        for (int rowIndex = 0; rowIndex < 2; rowIndex++) {
            LinearLayout grid = row();
            for (int columnIndex = 0; columnIndex < 2; columnIndex++) {
                int index = rowIndex * 2 + columnIndex;
                var palette = Palettes.ALL[index];
                LinearLayout card = column();
                pad(card, 9, 9, 9, 7);
                card.setBackground(ripple(PANEL, 15, index == paletteIndex ? LIME : LINE));
                card.setSelected(index == paletteIndex);
                card.setContentDescription(palette.name());
                LinearLayout colors = row();
                colors.setBackground(background(PANEL, 8, 0));
                colors.setClipToOutline(true);
                for (var swatch : palette.colors()) {
                    View chip = new View(this);
                    chip.setBackgroundColor(swatch.rgb());
                    colors.addView(chip, new LinearLayout.LayoutParams(0, MATCH, 1));
                }
                card.addView(colors, new LinearLayout.LayoutParams(MATCH, dp(15)));
                TextView title = text(palette.name(), 15, PAPER, true);
                pad(title, 1, 7, 0, 0);
                card.addView(title);
                cards.add(card);
                card.setOnClickListener(view -> {
                    if (paletteIndex == index) return;
                    paletteIndex = index;
                    paletteTitle.setText(palette.name());
                    description.setText(palette.description());
                    poster.setBackground(background(PALETTE_POSTER[index], 21, 0));
                    paletteArt.setArt(PALETTE_ART[index]);
                    pop(paletteArt);
                    for (int i = 0; i < cards.size(); i++) {
                        boolean selected = i == index;
                        cards.get(i).setBackground(ripple(PANEL, 15, selected ? LIME : LINE));
                        cards.get(i).setSelected(selected);
                    }
                });
                LinearLayout.LayoutParams box = new LinearLayout.LayoutParams(0, WRAP, 1);
                if (columnIndex == 1) box.setMarginStart(dp(8));
                grid.addView(card, box);
            }
            block(body, grid, 8);
        }

        LinearLayout timing = row();
        timing.setGravity(Gravity.CENTER_VERTICAL);
        timing.setBackground(background(PANEL, 18, LINE));
        pad(timing, 11, 8, 8, 8);
        timing.addView(text("Waktu mencari", 14, PAPER, true),
            new LinearLayout.LayoutParams(0, WRAP, 1));
        TextView duration = text(time(seconds), 17, PAPER, true);
        duration.setGravity(Gravity.CENTER);
        TextView minus = button("−", LINE, PAPER, () -> {});
        minus.setContentDescription("Kurangi waktu");
        TextView plus = button("+", LINE, PAPER, () -> {});
        plus.setContentDescription("Tambah waktu");
        Runnable update = () -> {
            duration.setText(time(seconds));
            enabled(minus, seconds > 10);
            enabled(plus, seconds < 120);
        };
        minus.setOnClickListener(view -> {
            seconds = Math.max(10, seconds - 5);
            update.run();
        });
        plus.setOnClickListener(view -> {
            seconds = Math.min(120, seconds + 5);
            update.run();
        });
        update.run();
        timing.addView(minus, new LinearLayout.LayoutParams(dp(48), dp(48)));
        timing.addView(duration, new LinearLayout.LayoutParams(dp(62), WRAP));
        timing.addView(plus, new LinearLayout.LayoutParams(dp(48), dp(48)));
        block(body, timing, 14);
        footer(button("Mulai misi  →", LIME, INK, this::beginRound));
    }

    private void roster() {
        header("PEMAIN", "Kembali", () -> show(Page.HOME));
        LinearLayout banner = row();
        banner.setGravity(Gravity.CENTER_VERTICAL);
        banner.setBackground(background(PANEL, 21, LINE));
        pad(banner, 14, 10, 8, 10);
        LinearLayout message = column();
        message.addView(display("Siapa ikut?", 35, PAPER));
        block(message, text("Masukkan nama 2–4 pemain.", 15, MUTED, false), 7);
        banner.addView(message, new LinearLayout.LayoutParams(0, WRAP, 1));
        StickerView team = new StickerView(this, StickerView.Art.TEAM);
        banner.addView(team, new LinearLayout.LayoutParams(dp(96), dp(84)));
        block(body, banner, 16);
        pop(team);
        for (int index = 0; index < players.size(); index++) {
            int slot = index;
            LinearLayout lane = row();
            lane.setGravity(Gravity.CENTER_VERTICAL);
            lane.setBackground(background(PANEL, 17, LINE));
            pad(lane, 10, 4, 6, 4);
            TextView number = tag(String.valueOf(index + 1), PALETTE_POSTER[index], INK);
            lane.addView(number);
            EditText field = new EditText(this);
            field.setTextColor(PAPER);
            field.setHintTextColor(MUTED);
            field.setTextSize(17);
            field.setBackgroundColor(Color.TRANSPARENT);
            field.setSingleLine();
            field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
            field.setFilters(new InputFilter[] { new InputFilter.LengthFilter(22) });
            field.setHint("Pemain " + (index + 1));
            field.setText(players.get(index));
            pad(field, 10, 4, 4, 4);
            field.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    players.set(slot, s.toString());
                }
                @Override public void afterTextChanged(Editable value) {}
            });
            lane.addView(field, new LinearLayout.LayoutParams(0, dp(52), 1));
            if (players.size() > 2) {
                TextView remove = button("×", LINE, PAPER, () -> {
                    players.remove(slot);
                    show(Page.PLAYERS);
                });
                remove.setContentDescription("Hapus " + player(slot));
                lane.addView(remove, new LinearLayout.LayoutParams(dp(48), dp(48)));
            }
            block(body, lane, 12);
        }
        if (players.size() < 4) {
            block(body, button("+ Tambah pemain", PANEL, LILAC, () -> {
                players.add("");
                show(Page.PLAYERS);
            }), 14);
        }
        footer(button("Mulai misi  →", LIME, INK, this::beginRound));
    }

    private void beginRound() {
        discardShots();
        swatchIndex = random.nextInt(Palettes.ALL[paletteIndex].colors().length);
        currentPlayer = 0;
        deadline = SystemClock.elapsedRealtime() + seconds * 1000L;
        show(Page.HUNT);
    }

    private void hunt() {
        header("TARGET MISI", "Keluar", this::confirmExit);
        block(body, text("Cari benda yang warnanya mendekati target.", 17, MUTED, false), 22);
        LinearLayout swatch = row();
        int color = target().rgb();
        int foreground = ColorMatch.light(color) ? Color.BLACK : Color.WHITE;
        swatch.setBackground(background(color, 24, 0));
        pad(swatch, 19, 16, 19, 17);
        swatch.setGravity(Gravity.CENTER_VERTICAL);
        swatch.setMinimumHeight(dp(150));
        LinearLayout swatchName = column();
        swatchName.addView(text(Palettes.ALL[paletteIndex].name(), 14, foreground, true));
        block(swatchName, display(target().name(), 33, foreground), 6);
        swatch.addView(swatchName, new LinearLayout.LayoutParams(0, WRAP, 1));
        StickerView targetArt = new StickerView(this, StickerView.Art.TARGET);
        swatch.addView(targetArt, new LinearLayout.LayoutParams(dp(101), dp(100)));
        pop(targetArt);
        block(body, swatch, 15);
        timerLabel = display("", 72, LIME);
        timerLabel.setGravity(Gravity.CENTER);
        timerLabel.setTypeface(Typeface.create("monospace", Typeface.BOLD));
        block(body, timerLabel, 16);
        FrameLayout track = new FrameLayout(this);
        track.setBackground(background(PANEL, 5, 0));
        track.setClipToOutline(true);
        huntProgress = new View(this);
        huntProgress.setBackgroundColor(LIME);
        huntProgress.setPivotX(0);
        track.addView(huntProgress, new FrameLayout.LayoutParams(MATCH, MATCH));
        LinearLayout.LayoutParams trackBox = new LinearLayout.LayoutParams(MATCH, dp(8));
        trackBox.setMargins(dp(24), dp(9), dp(24), dp(12));
        body.addView(track, trackBox);
        TextView first = text("Giliran pertama · " + player(0), 15, MUTED, true);
        first.setGravity(Gravity.CENTER);
        block(body, first, 4);
        footer(button("Buka kamera sekarang  →", LIME, INK, () -> show(Page.CAMERA)));
        clock.post(tick);
    }

    private void updateClock() {
        if (page != Page.HUNT) return;
        long remaining = deadline - SystemClock.elapsedRealtime();
        String shown = time((int) Math.max(0, (remaining + 999) / 1000));
        if (!shown.contentEquals(timerLabel.getText())) timerLabel.setText(shown);
        if (remaining <= 5_000) timerLabel.setTextColor(PEACH);
        huntProgress.setScaleX(Math.max(0f, remaining / (seconds * 1000f)));
        if (remaining <= 0) show(Page.CAMERA);
        else clock.postDelayed(tick, 100);
    }

    private void showCameraAccess(boolean denied) {
        stopCamera();
        body.removeAllViews();
        header("KAMERA", "Keluar", this::confirmExit);
        block(body, display("Aktifkan kamera", 36, PAPER), 24);
        block(body, text("Kroma Klub memakai kamera untuk membaca warna dan memotret temuan pemain.",
            17, MUTED, false), 12);
        block(body, button("Izinkan kamera", LIME, INK, () -> permission.launch(Manifest.permission.CAMERA)), 20);
        if (denied) {
            block(body, button("Buka pengaturan aplikasi", PANEL, PAPER, () ->
                startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    android.net.Uri.fromParts("package", getPackageName(), null)))), 10);
            for (int i = 0; i < body.getChildCount(); i++) enter(body.getChildAt(i), i * 35L);
        }
    }

    @Override protected void onResume() {
        super.onResume();
        if (page == Page.CAMERA && camera == null && body != null
                && checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            render();
        }
    }

    private void viewfinder() {
        header("GILIRAN " + (currentPlayer + 1) + "/" + players.size(), "Keluar", this::confirmExit);
        LinearLayout hud = column();
        pad(hud, 14, 10, 14, 12);
        hud.setBackground(background(INK, 15, 0));
        hud.addView(display(player(currentPlayer), 30, PAPER));
        hud.addView(text("Arahkan kotak bidik ke bendamu.", 14, MUTED, false));
        block(body, hud, 14);
        FrameLayout frame = new FrameLayout(this) {
            @Override protected void onMeasure(int widthSpec, int heightSpec) {
                super.onMeasure(widthSpec, MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthSpec),
                    MeasureSpec.EXACTLY));
            }
        };
        frame.setBackground(background(PANEL, 19, 0));
        frame.setClipToOutline(true);
        PreviewView preview = new PreviewView(this);
        preview.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);
        preview.setScaleType(PreviewView.ScaleType.FILL_CENTER);
        frame.addView(preview, new FrameLayout.LayoutParams(MATCH, MATCH));
        View reticle = new View(this) {
            private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            @Override protected void onDraw(Canvas c) {
                float size = getWidth() / 5f;
                float x = (getWidth() - size) / 2f;
                float y = (getHeight() - size) / 2f;
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(dp(4));
                paint.setColor(INK);
                c.drawRect(x, y, x + size, y + size, paint);
                paint.setStrokeWidth(dp(2));
                paint.setColor(Color.WHITE);
                c.drawRect(x, y, x + size, y + size, paint);
            }
        };
        frame.addView(reticle, new FrameLayout.LayoutParams(MATCH, MATCH));
        block(body, frame, 12);

        LinearLayout reading = row();
        reading.setGravity(Gravity.CENTER_VERTICAL);
        reading.setBackground(background(INK, 15, 0));
        pad(reading, 12, 9, 12, 9);
        View chip = new View(this);
        chip.setBackground(background(target().rgb(), 6, 0));
        reading.addView(chip, new LinearLayout.LayoutParams(dp(24), dp(24)));
        TextView goal = text(target().name(), 14, PAPER, true);
        pad(goal, 8, 0, 6, 0);
        reading.addView(goal, new LinearLayout.LayoutParams(0, WRAP, 1));
        TextView status = text("Menyiapkan…", 14, LIME, true);
        reading.addView(status);
        block(body, reading, 12);

        TextView snap = button("Ambil foto", LIME, INK, () -> {});
        TextView retry = button("Muat ulang kamera", INK, PAPER, this::render);
        block(body, retry, 8);
        enabled(snap, false);
        footer(snap);
        snap.setOnClickListener(view -> {
            if (camera != null) {
                enabled(snap, false);
                status.setText(R.string.camera_capturing);
                camera.takePhoto();
            }
        });

        LiveCamera active = new LiveCamera(this, preview, new LiveCamera.Listener() {
            @Override public void onColor(int color) {
                tintCamera(color);
                if (retry.getVisibility() == View.GONE) return;
                retry.setVisibility(View.GONE);
                status.setText(R.string.camera_ready);
                enabled(snap, true);
            }
            @Override public void onPhoto(Bitmap photo, int color) {
                savePhoto(photo, color);
            }
            @Override public void onCameraError(String message) {
                stopCamera();
                status.setText(message);
                status.setTextSize(12);
                snap.setText(R.string.camera_retry);
                snap.setOnClickListener(view -> render());
                retry.setVisibility(View.GONE);
                enabled(snap, true);
            }
        });
        camera = active;
        preview.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override public void onLayoutChange(View view, int left, int top, int right, int bottom,
                    int oldLeft, int oldTop, int oldRight, int oldBottom) {
                var viewPort = preview.getViewPort();
                if (viewPort == null) return;
                preview.removeOnLayoutChangeListener(this);
                if (camera == active) active.start(viewPort);
            }
        });
    }

    private void savePhoto(Bitmap photo, int color) {
        try {
            Shot shot = new Shot(photos.save(photo), color);
            if (currentPlayer == shots.size()) {
                shots.add(shot);
            } else {
                photos.remove(shots.set(currentPlayer, shot).file());
            }
            show(Page.REVIEW);
        } catch (IOException error) {
            Log.w("KromaKlub", "Unable to save photo", error);
            Toast.makeText(this, "Foto belum tersimpan. Coba lagi.", Toast.LENGTH_SHORT).show();
            render();
        } finally {
            photo.recycle();
        }
    }

    private void stopCamera() {
        if (cameraTint != null) {
            cameraTint.cancel();
            cameraTint = null;
        }
        if (camera != null) {
            camera.close();
            camera = null;
        }
    }

    private void review() {
        Shot shot = shots.get(currentPlayer);
        setBackdrop(shot.color());
        header("FOTO " + (currentPlayer + 1) + "/" + players.size(), "Keluar", this::confirmExit);

        LinearLayout card = column();
        pad(card, 10, 10, 10, 13);
        card.setBackground(background(PAPER, 23, 0));
        card.setElevation(dp(4));
        card.setRotation(-2f);
        ImageView image = picture(shot, player(currentPlayer));
        card.addView(image, new LinearLayout.LayoutParams(MATCH, dp(300)));
        LinearLayout caption = row();
        caption.setGravity(Gravity.CENTER_VERTICAL);
        pad(caption, 4, 12, 4, 0);
        TextView name = display(player(currentPlayer), 25, INK);
        caption.addView(name, new LinearLayout.LayoutParams(0, WRAP, 1));
        caption.addView(tag("Siap dikunci", LIME, INK));
        card.addView(caption);
        block(body, card, 18);
        pop(card);
        TextView revealHint = tag("Skor dibuka setelah foto terakhir dikunci", INK, PAPER);
        revealHint.setGravity(Gravity.CENTER);
        block(body, revealHint, 14);
        block(body, button("Foto ulang", INK, PAPER, () -> show(Page.CAMERA)), 12);

        boolean last = currentPlayer == players.size() - 1;
        if (last && players.size() < 4) {
            block(body, button("Kunci & tambah pemain", INK, PAPER, this::addLatePlayer), 8);
        }
        footer(button(last ? "Kunci foto & lihat hasil" : "Kunci & lanjut ke " + player(currentPlayer + 1),
            LIME, INK, () -> {
                if (last) show(Page.RESULTS);
                else {
                    currentPlayer++;
                    show(Page.CAMERA);
                }
            }));
    }

    private void addLatePlayer() {
        EditText field = new EditText(this);
        field.setSingleLine();
        field.setHint("Pemain " + (players.size() + 1));
        field.setFilters(new InputFilter[] { new InputFilter.LengthFilter(22) });
        var dialog = new android.app.AlertDialog.Builder(this)
            .setTitle("Tambah pemain")
            .setView(field)
            .setNegativeButton("Batal", null)
            .setPositiveButton("Lanjut", (choice, which) -> {
                players.add(field.getText().toString());
                currentPlayer++;
                show(Page.CAMERA);
            })
            .create();
        field.requestFocus();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
        dialog.show();
    }

    private void results() {
        header("HASIL MISI", "Tutup", this::returnToLobby);
        int[] colors = new int[shots.size()];
        for (int i = 0; i < colors.length; i++) colors[i] = shots.get(i).color();
        List<RoundRanking.Entry> ordered = RoundRanking.reveal(target().rgb(), players.size(), colors);
        int best = ordered.get(0).player();
        boolean tie = ordered.get(1).score() == ordered.get(0).score();
        LinearLayout podium = row();
        podium.setGravity(Gravity.CENTER_VERTICAL);
        podium.setBackground(background(0xFFFFD677, 22, 0));
        pad(podium, 14, 12, 11, 12);
        LinearLayout podiumLabel = column();
        podiumLabel.addView(display(tie ? "Seri di puncak!" : player(best) + " juara!", 33, INK));
        TextView color = tag("Target · " + target().name(), target().rgb(),
            ColorMatch.light(target().rgb()) ? Color.BLACK : Color.WHITE);
        LinearLayout.LayoutParams label = new LinearLayout.LayoutParams(WRAP, WRAP);
        label.topMargin = dp(9);
        podiumLabel.addView(color, label);
        podium.addView(podiumLabel, new LinearLayout.LayoutParams(0, WRAP, 1));
        FrameLayout emblem = new FrameLayout(this);
        if (!tie) {
            ImageView winner = picture(shots.get(best), player(best));
            FrameLayout.LayoutParams tile = new FrameLayout.LayoutParams(dp(84), dp(84), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            winner.setRotation(5f);
            emblem.addView(winner, tile);
        }
        StickerView crown = new StickerView(this, StickerView.Art.TROPHY);
        emblem.addView(crown, new FrameLayout.LayoutParams(dp(76), dp(68), Gravity.TOP | Gravity.CENTER_HORIZONTAL));
        podium.addView(emblem, new LinearLayout.LayoutParams(dp(107), dp(tie ? 75 : 116)));
        pop(crown);
        block(body, podium, 15);

        for (int place = 0; place < ordered.size(); place++) {
            var result = ordered.get(place);
            int index = result.player();
            Shot shot = shots.get(index);
            int rank = place + 1;
            while (rank > 1 && ordered.get(rank - 2).score() == result.score()) rank--;
            LinearLayout card = row();
            card.setGravity(Gravity.CENTER_VERTICAL);
            card.setBackground(ripple(PANEL, 18, rank == 1 ? LILAC : LINE));
            card.setOnClickListener(view -> {
                ImageView full = picture(shot, player(index));
                full.setScaleType(ImageView.ScaleType.FIT_CENTER);
                full.setAdjustViewBounds(true);
                full.setMaxHeight(dp(420));
                new android.app.AlertDialog.Builder(this)
                    .setTitle(player(index) + " · " + result.score() + "% cocok")
                    .setView(full)
                    .setPositiveButton("Tutup", null)
                    .show();
            });
            pad(card, 9, 9, 11, 9);
            card.addView(picture(shot, player(index)), new LinearLayout.LayoutParams(dp(66), dp(66)));

            LinearLayout details = column();
            pad(details, 10, 0, 0, 0);
            LinearLayout stats = row();
            stats.setGravity(Gravity.CENTER_VERTICAL);
            TextView name = text(rank + ". " + player(index), 15, PAPER, true);
            stats.addView(name, new LinearLayout.LayoutParams(0, WRAP, 1));
            TextView percentage = text(result.score() + "%", 19, rank == 1 ? LIME : PAPER, true);
            stats.addView(percentage);
            details.addView(stats);
            LinearLayout track = row();
            track.setBackground(background(INK, 8, 0));
            track.setClipToOutline(true);
            View bar = new View(this);
            bar.setBackgroundColor(shot.color());
            track.setWeightSum(100);
            track.addView(bar, new LinearLayout.LayoutParams(0, MATCH, result.score()));
            bar.setPivotX(0);
            bar.setScaleX(0f);
            long delay = 215L + place * 90L;
            bar.animate().scaleX(1f).setStartDelay(delay).setDuration(390).start();
            percentage.setAlpha(0f);
            percentage.animate().alpha(1f).setStartDelay(delay + 100).setDuration(250).start();
            LinearLayout.LayoutParams line = new LinearLayout.LayoutParams(MATCH, dp(8));
            line.topMargin = dp(7);
            details.addView(track, line);
            card.addView(details, new LinearLayout.LayoutParams(0, WRAP, 1));
            block(body, card, 9);
        }
        block(body, button("Main lagi", PANEL, PAPER, this::returnToLobby), 13);
        footer(button("Bagikan hasil", LIME, INK, () -> shareResults(ordered)));
    }

    private void shareResults(List<RoundRanking.Entry> ordered) {
        Bitmap recap = Bitmap.createBitmap(720, 154 + ordered.size() * 148, Bitmap.Config.ARGB_8888);
        try {
            Canvas paper = new Canvas(recap);
            paper.drawColor(INK);
            Paint ink = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
            ink.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
            ink.setColor(LIME);
            ink.setTextSize(50);
            paper.drawText("KROMA KLUB", 36, 68, ink);
            ink.setColor(target().rgb());
            paper.drawRoundRect(new RectF(38, 96, 68, 126), 6, 6, ink);
            ink.setColor(PAPER);
            ink.setTextSize(28);
            paper.drawText("Target · " + target().name(), 83, 120, ink);

            for (int i = 0; i < ordered.size(); i++) {
                var result = ordered.get(i);
                int index = result.player();
                Shot shot = shots.get(index);
                int top = 148 + 148 * i;
                Rect imageBox = new Rect(38, top, 158, top + 120);
                ink.setColor(shot.color());
                paper.drawRect(imageBox, ink);
                Bitmap image = photos.load(shot.file());
                if (image != null) {
                    paper.drawBitmap(image, null, imageBox, ink);
                    image.recycle();
                }
                ink.setColor(PAPER);
                ink.setTextSize(31);
                paper.drawText(player(index), 179, top + 49, ink);
                ink.setColor(LIME);
                ink.setTextSize(28);
                paper.drawText(result.score() + "% cocok", 179, top + 89, ink);
            }
            File folder = new File(getCacheDir(), "share");
            folder.mkdirs();
            File file = File.createTempFile("kroma-klub-", ".jpg", folder);
            try (FileOutputStream output = new FileOutputStream(file)) {
                if (!recap.compress(Bitmap.CompressFormat.JPEG, 93, output)) {
                    throw new IOException("Could not encode result");
                }
            }
            var uri = FileProvider.getUriForFile(this, getPackageName() + ".share", file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("image/jpeg");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.setClipData(ClipData.newUri(getContentResolver(), "Hasil Kroma Klub", uri));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Bagikan hasil Kroma Klub"));
        } catch (IOException error) {
            Toast.makeText(this, "Hasil belum bisa dibagikan.", Toast.LENGTH_SHORT).show();
        } finally {
            recap.recycle();
        }
    }

    private ImageView picture(Shot shot, String name) {
        ImageView image = new ImageView(this);
        image.setContentDescription("Foto " + name);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setBackground(background(shot.color(), 11, 0));
        image.setClipToOutline(true);
        image.setImageBitmap(photos.load(shot.file()));
        return image;
    }

    private void confirmExit() {
        new android.app.AlertDialog.Builder(this)
            .setTitle("Akhiri misi?")
            .setMessage("Foto dan skor ronde ini akan dihapus.")
            .setNegativeButton("Lanjut bermain", null)
            .setPositiveButton("Akhiri misi", (dialog, which) -> returnToLobby())
            .show();
    }

    private void discardShots() {
        for (Shot shot : shots) photos.remove(shot.file());
        shots.clear();
    }

    private void returnToLobby() {
        discardShots();
        show(Page.HOME);
    }

    private String player(int index) {
        String name = players.get(index).trim();
        return name.isEmpty() ? "Pemain " + (index + 1) : name;
    }

    private Palettes.Swatch target() {
        return Palettes.ALL[paletteIndex].colors()[swatchIndex];
    }

    private String time(int duration) {
        return String.format(Locale.ROOT, "%02d:%02d", duration / 60, duration % 60);
    }

    private void header(String title, String action, Runnable onAction) {
        LinearLayout header = row();
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView label = tag(title, LILAC, INK);
        label.setLetterSpacing(0.07f);
        header.addView(label);
        header.addView(new View(this), new LinearLayout.LayoutParams(0, 0, 1));
        header.addView(button(action, INK, PAPER, onAction), new LinearLayout.LayoutParams(WRAP, dp(48)));
        body.addView(header);
    }

    private void footer(TextView action) {
        LinearLayout foot = column();
        pad(foot, 20, 9, 20, 12);
        foot.addView(action, new LinearLayout.LayoutParams(MATCH, WRAP));
        shell.addView(foot);
        enter(foot, 115);
    }

    private void block(LinearLayout parent, View child, int gap) {
        LinearLayout.LayoutParams item = new LinearLayout.LayoutParams(MATCH, WRAP);
        item.topMargin = dp(gap);
        parent.addView(child, item);
    }

    private LinearLayout row() {
        return new LinearLayout(this);
    }

    private LinearLayout column() {
        LinearLayout group = new LinearLayout(this);
        group.setOrientation(LinearLayout.VERTICAL);
        return group;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView label = new TextView(this);
        label.setText(value);
        label.setTextColor(color);
        label.setTextSize(size);
        label.setIncludeFontPadding(false);
        if (bold) label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return label;
    }

    private TextView display(String value, int size, int color) {
        TextView label = text(value, size, color, true);
        label.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        label.setLetterSpacing(-0.03f);
        return label;
    }

    private TextView tag(String value, int fill, int foreground) {
        TextView label = text(value, 13, foreground, true);
        label.setBackground(background(fill, 9, 0));
        pad(label, 10, 8, 10, 8);
        return label;
    }

    private TextView button(String value, int fill, int foreground, Runnable action) {
        TextView control = new android.widget.Button(this);
        control.setText(value);
        control.setTextColor(foreground);
        control.setTextSize(16);
        control.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        control.setAllCaps(false);
        control.setGravity(Gravity.CENTER);
        control.setMinimumHeight(dp(48));
        control.setMinWidth(0);
        control.setMinimumWidth(0);
        control.setBackground(ripple(fill, 15, 0));
        pad(control, 12, 10, 12, 10);
        control.setOnClickListener(view -> action.run());
        return control;
    }

    private void enabled(TextView control, boolean enabled) {
        control.setEnabled(enabled);
        control.setAlpha(enabled ? 1f : 0.55f);
    }

    private void enter(View view, long delay) {
        view.setAlpha(0f);
        view.setTranslationY(dp(13));
        view.animate().alpha(1f).translationY(0f).setStartDelay(delay).setDuration(270)
            .setInterpolator(new DecelerateInterpolator()).start();
    }

    private void pop(View view) {
        view.animate().cancel();
        view.setScaleX(0.82f);
        view.setScaleY(0.82f);
        view.animate().scaleX(1f).scaleY(1f).setDuration(380)
            .setInterpolator(new OvershootInterpolator(1.45f)).start();
    }

    private void tintCamera(int color) {
        if (cameraTint != null) cameraTint.cancel();
        int from = backdrop;
        cameraTint = ValueAnimator.ofFloat(0f, 1f);
        cameraTint.setDuration(130);
        cameraTint.addUpdateListener(animation -> setBackdrop(ColorUtils.blendARGB(from, color,
            (float) animation.getAnimatedValue())));
        cameraTint.start();
    }

    private void setBackdrop(int color) {
        backdrop = color;
        canvas.setBackgroundColor(color);
        boolean light = ColorMatch.light(color);
        if (light == darkSystemIcons) return;
        darkSystemIcons = light;
        var bars = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        bars.setAppearanceLightStatusBars(light);
        bars.setAppearanceLightNavigationBars(light);
    }

    private GradientDrawable background(int fill, int radius, int border) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(fill);
        shape.setCornerRadius(dp(radius));
        if (border != 0) shape.setStroke(dp(1), border);
        return shape;
    }

    private RippleDrawable ripple(int fill, int radius, int border) {
        return new RippleDrawable(ColorStateList.valueOf(0x337F72AB), background(fill, radius, border), null);
    }

    private void pad(View view, int left, int top, int right, int bottom) {
        view.setPadding(dp(left), dp(top), dp(right), dp(bottom));
    }

    private int dp(float value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }
}
