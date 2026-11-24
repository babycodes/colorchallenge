package id.kromaklub;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.os.SystemClock;
import android.util.Log;

import androidx.activity.ComponentActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.UseCaseGroup;
import androidx.camera.core.ViewPort;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

final class LiveCamera implements AutoCloseable {
    interface Listener {
        void onColor(int color);
        void onPhoto(Bitmap photo, int color);
        void onCameraError(String message);
    }

    private final ComponentActivity activity;
    private final PreviewView view;
    private final Listener listener;
    private final Executor main;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final AtomicBoolean capture = new AtomicBoolean();
    private ProcessCameraProvider provider;
    private Preview preview;
    private ImageAnalysis analysis;
    private volatile boolean closed;
    private long nextSampleAt;

    LiveCamera(ComponentActivity activity, PreviewView view, Listener listener) {
        this.activity = activity;
        this.view = view;
        this.listener = listener;
        main = ContextCompat.getMainExecutor(activity);
    }

    void start(ViewPort viewPort) {
        var ready = ProcessCameraProvider.getInstance(activity);
        ready.addListener(() -> {
            if (closed) return;
            try {
                provider = ready.get();
                CameraSelector camera = provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)
                    ? CameraSelector.DEFAULT_BACK_CAMERA : CameraSelector.DEFAULT_FRONT_CAMERA;
                preview = new Preview.Builder().build();
                preview.setSurfaceProvider(view.getSurfaceProvider());
                analysis = new ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build();
                analysis.setAnalyzer(worker, this::analyze);
                UseCaseGroup group = new UseCaseGroup.Builder()
                    .setViewPort(viewPort)
                    .addUseCase(preview)
                    .addUseCase(analysis)
                    .build();
                provider.bindToLifecycle(activity, camera, group);
            } catch (Exception error) {
                Log.w("KromaCamera", "Unable to bind camera", error);
                listener.onCameraError("Kamera belum bisa dibuka. Coba lagi.");
            }
        }, main);
    }

    void takePhoto() {
        capture.set(true);
    }

    private void analyze(ImageProxy frame) {
        try (frame) {
            if (closed) return;
            boolean save = capture.getAndSet(false);
            long now = SystemClock.elapsedRealtime();
            if (!save && now < nextSampleAt) return;
            nextSampleAt = now + 150;

            Bitmap bitmap = frame.toBitmap();
            Rect crop = frame.getCropRect();
            int side = Math.min(crop.width(), crop.height());
            int squareLeft = crop.centerX() - side / 2;
            int squareTop = crop.centerY() - side / 2;
            int sampleSide = Math.max(1, side / 5);
            int[] pixels = new int[sampleSide * sampleSide];
            bitmap.getPixels(pixels, 0, sampleSide, crop.centerX() - sampleSide / 2,
                crop.centerY() - sampleSide / 2, sampleSide, sampleSide);
            int color = ColorMatch.average(pixels);
            Bitmap photo = null;
            if (save) {
                Matrix rotation = new Matrix();
                rotation.postRotate(frame.getImageInfo().getRotationDegrees());
                photo = Bitmap.createBitmap(bitmap, squareLeft, squareTop, side, side, rotation, true);
            }
            if (bitmap != photo) bitmap.recycle();
            Bitmap captured = photo;
            main.execute(() -> {
                if (closed) {
                    if (captured != null) captured.recycle();
                } else if (captured != null) {
                    listener.onPhoto(captured, color);
                } else {
                    listener.onColor(color);
                }
            });
        } catch (RuntimeException error) {
            Log.w("KromaCamera", "Unable to analyze frame", error);
            main.execute(() -> {
                if (!closed) listener.onCameraError("Gambar dari kamera belum bisa dibaca. Coba lagi.");
            });
        }
    }

    @Override public void close() {
        closed = true;
        if (analysis != null) analysis.clearAnalyzer();
        if (provider != null && preview != null && analysis != null) provider.unbind(preview, analysis);
        worker.shutdown();
    }
}
