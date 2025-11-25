# KameraKu

KameraKu adalah proyek sederhana untuk eksperimen fitur kamera dan pemrosesan gambar di Android. README ini fokus pada tiga topik penting untuk aplikasi kamera:

- Alur izin (runtime permissions)
- Menyimpan gambar menggunakan MediaStore (scoped storage)
- Menangani rotasi gambar (Exif / sensor / device rotation)

---

## Ringkasan singkat

- Pastikan aplikasi meminta izin kamera dan izin akses media yang sesuai menurut versi Android.
- Gunakan MediaStore untuk menyimpan gambar ke galeri/penyimpanan publik (khususnya Android Q+ dengan scoped storage).
- Normalisasi rotasi gambar sebelum menampilkan atau menyimpan agar orientasi selalu benar.

---

## Alur Izin (Permission Flow)

1. Tentukan daftar izin yang diperlukan sesuai API level:
   - CAMERA — wajib untuk akses kamera.
   - Android 13 (TIRAMISU/API 33+) memperkenalkan READ_MEDIA_IMAGES untuk membaca gambar.
   - Untuk API < 33 gunakan READ_EXTERNAL_STORAGE; untuk API < Q (API 29) mungkin masih perlu WRITE_EXTERNAL_STORAGE jika aplikasi menulis ke penyimpanan eksternal secara langsung.

2. Periksa apakah izin sudah diberikan:
   - Jika sudah, lanjutkan membuka kamera.
   - Jika belum, tunjukkan rationale (opsional) ketika perlu, lalu minta izin runtime.

3. Tangani hasil permintaan izin:
   - Jika ditolak sementara, tampilkan pesan/rationale atau opsi ke pengaturan.
   - Jika ditolak permanent (dont ask again), arahkan pengguna untuk membuka Settings.
