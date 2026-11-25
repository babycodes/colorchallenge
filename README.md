# Kroma Klub

Game Android native berbahasa Indonesia untuk 2–4 pemain yang bergiliran memakai satu HP. Aplikasi mendukung Android 6.0 (API 23) atau lebih baru dan menggunakan CameraX dengan backend Camera2. Permainan berjalan offline.

## Cara bermain

1. Geser kartu atau gunakan tombol panah untuk memilih palet **Spektrum**, **Pesisir**, **Pasar Sore**, atau **Awan Gula**. Setiap palet memuat 16 warna target. Ketuk tombol pemain di kanan atas untuk mengubah nama atau jumlah pemain.
2. Atur durasi 20–120 detik dan ketuk **Mulai misi** untuk melihat target bersama. Setelah siap, ketuk **Mulai mencari** untuk memulai hitung mundur. Tombol **Buka kamera** muncul setelah 20 detik berlalu dan tetap tersedia ketika hitungan mencapai nol.
3. Arahkan kotak bidik ke bagian benda yang mewakili warnanya. Latar mengikuti warna rata-rata di kotak bidik. Ketuk **Ambil foto**, periksa temuanmu, lalu kunci foto untuk lanjut ke pemain berikutnya.
4. Skor dan peringkat dihitung saat foto terakhir dikunci. Bandingkan skor dan foto semua pemain. **Bagikan hasil** membuat gambar ringkasan lewat menu berbagi Android.

Skor dihitung dari jarak warna CIELAB antara target dan area tengah frame. Semakin kecil jaraknya, semakin tinggi skornya. Foto disimpan sementara di cache aplikasi.

## Build APK

Butuh JDK 17 atau 21 dan Android SDK dengan paket `platforms;android-35` serta `build-tools;35.0.0`. Setel `ANDROID_HOME` ke direktori SDK atau buka repo di Android Studio agar lokasi SDK terdeteksi.

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug
```

APK debug tersedia di `app/build/outputs/apk/debug/app-debug.apk`. Untuk memasangnya di HP dengan USB debugging aktif:

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Workflow **Android APK** di GitHub Actions menjalankan unit test dan lint, membangun APK, dan mengunggahnya sebagai artifact **kroma-klub-debug**. Workflow berjalan saat push, pull request, atau dijalankan manual dari tab **Actions**.
