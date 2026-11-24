package id.kromaklub;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

final class PhotoStore {
    private final File directory;

    PhotoStore(Context context) {
        directory = new File(context.getCacheDir(), "rounds");
        directory.mkdirs();
    }

    String save(Bitmap photo) throws IOException {
        File picture = File.createTempFile("photo-", ".jpg", directory);
        try (FileOutputStream output = new FileOutputStream(picture)) {
            if (!photo.compress(Bitmap.CompressFormat.JPEG, 93, output)) {
                throw new IOException("JPEG compression failed");
            }
        } catch (IOException error) {
            picture.delete();
            throw error;
        }
        return picture.getName();
    }

    Bitmap load(String file) {
        return BitmapFactory.decodeFile(new File(directory, file).getAbsolutePath());
    }

    void remove(String file) {
        new File(directory, file).delete();
    }
}
