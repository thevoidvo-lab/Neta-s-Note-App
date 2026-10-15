# Neta's Notes

Notes app full-native dengan freeform canvas editor, custom home (judul/bg/font/sticker/icon), PIN lock, dan export/import JSON.

## Cara build (GitHub Actions, gak perlu laptop)

1. Bikin repo GitHub baru, upload semua isi folder ini (struktur folder tetap sama)
2. Masuk tab **Actions** → workflow **"Build Neta's Notes APK"** bakal jalan otomatis (atau klik **Run workflow**)
3. Tunggu 3-5 menit → download APK dari bagian **Artifacts**
4. Install di HP (aktifin "Install from unknown sources" dulu)

Workflow ini sengaja **gak pakai gradlew** (biar gak kejadian lagi kayak error gradlew corrupt kemarin) — dia install Gradle 8.0 langsung lewat `gradle/actions/setup-gradle`, dipin ke versi yang cocok sama AGP 8.1.0.

## Fitur yang udah lengkap

- Home: search, filter chips (Semua/Pinned/Locked), card catatan dengan pin/lock/swipe, FAB, empty state, bottom nav
- Editor freeform: canvas bebas, drag/resize (pinch)/rotate (2 jari) tiap elemen, tap pilih, double-tap edit teks, long-press menu (duplikat/kunci/sembunyikan/hapus), undo/redo (max 100), auto-save tiap 5 detik
- Elemen: title, text, image, sticker, shape — tiap elemen bisa ganti font size, warna, bold/italic/underline, alignment
- Settings lengkap: custom judul Home, custom background Home (solid/gradient/gambar/blur), custom font Home (upload .ttf/.otf), custom sticker Home, ganti PIN (EncryptedSharedPreferences), 6 preset ikon app (activity-alias), export/import JSON
- Database Room sesuai skema yang diminta, error handling di semua titik yang diminta (gambar/font/JSON gagal → fallback, PIN salah 5x → tutup, dst)

## Beberapa penyesuaian dari spek awal (biar build reliable)

- **Grid & snap**: diimplementasi sebagai toggle sederhana (garis grid + snap ke kelipatan 20dp), bukan sistem snap-guide visual yang kompleks kayak Figma
- **Pan canvas 2 jari**: translasi sederhana (geser seluruh canvas), bukan matrix zoom-pan penuh dengan pinch-zoom canvas terpisah dari pinch-resize elemen
- **Sticker Home**: tersimpan & tampil, tapi drag-reposition langsung di halaman Home belum ada UI-nya (posisi diset default saat upload) — kalau mau fitur drag sticker di Home, bisa nanti ditambahin StickerOverlayView yang interactive
- **Ikon preset**: 6 ikon dibikin dari vector drawable simpel (bukan ilustrasi PNG detail), biar gak perlu asset gambar eksternal
- **res/font**: gak diisi font Poppins/Inter beneran (file binary gak bisa digenerate dari sini) — app pakai system font default, dan font custom yang diupload user tetap jalan penuh lewat upload .ttf/.otf di Settings

Kalau nanti ada fitur yang mau ditambah/diperbaiki dari daftar di atas, atau ada error pas build/jalan, kirim detail errornya — gampang dibenerin satu-satu kayak kemarin.
