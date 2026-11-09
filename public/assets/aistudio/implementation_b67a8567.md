# implementation.md — Aplikasi Manajemen Keuangan Pribadi (Android)

> Draft v0.1 · 9 Nov 2026 · Nama aplikasi: belum ditentukan · Package sementara: `com.example.keuangan`

Dokumen ini menggabungkan PRD ringkas dan rencana implementasi. Kerjakan per fase (bagian 6), centang checklist setelah tesnya lulus, dan perbarui dokumen ini kalau ada keputusan yang berubah.

## 1. Ringkasan

Aplikasi Android offline untuk mencatat pemasukan dan pengeluaran, memantau saldo semua akun (kas, bank, e-wallet), mengatur anggaran per periode, dan melihat laporan arus kas. Dipakai satu orang; data tersimpan di perangkat, tanpa login dan tanpa server.

**Tujuan v1.0**

- Mencatat satu transaksi dalam kurang dari 10 detik: `+` → ketik nominal → pilih kategori → Simpan (akun dan tanggal sudah terisi otomatis).
- Saldo setiap akun selalu sama dengan hasil hitung dari transaksinya (tidak ada angka saldo yang disimpan terpisah).
- Sisa anggaran periode berjalan terlihat di Beranda tanpa pindah layar.
- Seluruh data bisa di-backup dan dipulihkan tanpa kehilangan satu transaksi pun.

**Bukan tujuan v1.0:** sinkronisasi antar perangkat, berbagi dengan pasangan/keluarga, integrasi langsung ke bank, multi-currency, scan struk, pencatatan otomatis dari notifikasi/SMS, iklan, dan analytics pihak ketiga.

**Ukuran keberhasilan** (diukur sendiri setelah 1 bulan pakai)

| Ukuran | Target |
| --- | --- |
| Hari dalam sebulan yang punya minimal 1 transaksi tercatat | ≥ 80% |
| Waktu rata-rata mencatat satu transaksi (10 percobaan, pakai stopwatch) | < 10 detik |

## 2. Asumsi

Koreksi kalau ada yang salah; kolom kanan menunjukkan apa yang berubah.

| # | Asumsi | Kalau salah |
| --- | --- | --- |
| A1 | Pengguna tunggal, tanpa akun/login | Berbagi atau sinkron butuh backend, arsitektur berubah |
| A2 | Rupiah (IDR) saja | Multi-currency mengubah model uang dan laporan |
| A3 | Offline; backup ke file lewat pemilih file Android | Sinkron cloud butuh izin internet dan server |
| A4 | Dipasang pribadi (APK), belum ke Play Store | Rilis publik butuh kebijakan privasi dan review izin sensitif |
| A5 | Native Kotlin + Jetpack Compose | Lintas platform mengubah seluruh bagian 3 |
| A6 | Pencatatan otomatis dari notifikasi/SMS ditunda ke backlog | Kalau masuk v1: izin sensitif dan fase riset tambahan |
| A7 | minSdk 26 (Android 8.0), sehingga `java.time` tersedia tanpa desugaring | Sesuaikan dengan versi Android di HP yang dipakai |

## 3. Tech stack & struktur proyek

| Area | Pilihan | Catatan |
| --- | --- | --- |
| Bahasa & UI | Kotlin, Jetpack Compose, Material 3 | Tema terang/gelap/ikuti sistem |
| Arsitektur | MVVM, satu `StateFlow` UI state per layar | Lapisan `ui` → `domain` (use case) → `data` |
| DI | Hilt | |
| Database | Room (SQLite), query reaktif lewat `Flow` | `exportSchema = true`, auto-migration + tes migrasi |
| Preferensi | DataStore | Tema, tanggal mulai periode, pengaturan kunci |
| Background | WorkManager + Coroutines | Transaksi berulang, pengingat, backup berkala |
| Navigasi | Navigation Compose | |
| Keamanan | BiometricPrompt, PIN ber-hash, Android Keystore | Detail di bagian 7 |
| Grafik | Library chart Compose (mis. Vico) atau Canvas sendiri | Putuskan di Fase 2 |
| Tes | JUnit, MockK, Turbine, Room in-memory, Compose UI test | |
| Build | Gradle Kotlin DSL + version catalog | Pakai SDK stabil terbaru saat mulai; naikkan `targetSdk` saat rilis agar sesuai persyaratan Google Play bila nanti dipublikasikan. Nomor versi tidak dikunci di dokumen ini |

Struktur proyek (package-by-feature; tiap feature berisi subpaket `ui`, `domain`, `data`):

```text
app/src/main/java/<package>/
├── core/
│   ├── database/    # AppDatabase, DAO, converter, migrasi
│   ├── datastore/   # preferensi
│   ├── money/       # Money: Long rupiah, format "Rp 1.250.000", parser "25rb"
│   ├── time/        # Clock yang bisa disuntik, PeriodResolver
│   └── ui/          # komponen bersama: AmountField, CategoryChip, EmptyState
├── feature/
│   ├── onboarding/
│   ├── home/
│   ├── transaction/ # form, daftar, detail, filter & cari
│   ├── account/     # daftar, detail, transfer, rekonsiliasi
│   ├── category/
│   ├── budget/
│   ├── report/
│   ├── recurring/
│   ├── backup/      # backup, restore, ekspor/impor CSV
│   └── settings/    # kunci aplikasi, tema, periode
├── navigation/
└── MainActivity.kt
```

## 4. Model data & aturan bisnis

### 4.1 Aturan umum

- Uang = `Long` dalam rupiah (tanpa desimal). Jangan pakai `Double`/`Float`.
- `amount` selalu positif; arah ditentukan oleh `type`.
- Tanggal transaksi = `LocalDate` yang disimpan sebagai `epochDay: Int` supaya tidak bergeser karena zona waktu. `createdAt`/`updatedAt` = epoch millis.
- ID = UUID `String` (memudahkan restore dan penggabungan backup). Enum disimpan sebagai `String`.
- Hindari nama tabel `transaction`; pakai `transactions`.
- Saldo tidak disimpan. Saldo akun = `initialBalance` + pemasukan − pengeluaran − transfer keluar + transfer masuk, dihitung lewat query `SUM`.

### 4.2 Sketsa entity (bukan kode final)

```kotlin
Account(
    id, name, type /* CASH, BANK, EWALLET, CREDIT, SAVINGS, OTHER */,
    initialBalance: Long,              // boleh negatif (kartu kredit/PayLater)
    includeInTotal: Boolean = true,
    colorHex: String?, icon: String?, sortOrder: Int,
    isArchived: Boolean = false, createdAt: Long
)

Category(
    id, name, kind /* EXPENSE, INCOME */, parentId: String?,   // satu level sub-kategori
    colorHex: String?, icon: String?, sortOrder: Int,
    isArchived: Boolean = false, isSystem: Boolean = false
)

TransactionEntity(                     // tabel "transactions"
    id, type /* EXPENSE, INCOME, TRANSFER */, amount: Long,    // amount > 0
    accountId,                         // sumber, FK RESTRICT
    toAccountId: String?,              // hanya TRANSFER, FK RESTRICT
    categoryId: String?,               // null jika TRANSFER, FK RESTRICT
    date: Int /* epochDay */, note: String?,
    recurringRuleId: String?, createdAt: Long, updatedAt: Long
)  // indeks: (date), (accountId, date), (categoryId, date)

Budget(
    id, periodKey: String /* "2026-11" */,
    categoryId: String? /* null = anggaran total */, amount: Long
)  // unik (periodKey, categoryId). SQLite menganggap NULL berbeda satu sama lain,
   // jadi keunikan anggaran total ditegakkan di DAO (upsert), bukan di indeks.

RecurringRule(
    id, type, amount: Long, accountId, toAccountId: String?, categoryId: String?, note: String?,
    frequency /* DAILY, WEEKLY, MONTHLY, YEARLY */, interval: Int = 1,
    startDate: Int, endDate: Int?, autoPost: Boolean,
    remindDaysBefore: Int?, lastGeneratedDate: Int?, isActive: Boolean = true
)
```

### 4.3 Aturan bisnis

1. **Transfer** bukan pemasukan/pengeluaran: tidak dihitung di Beranda, anggaran, maupun laporan. Biaya transfer dicatat sebagai pengeluaran terpisah (kategori "Biaya admin & pajak").
2. **Validasi:** `amount > 0`; pengeluaran/pemasukan wajib punya akun dan kategori; transfer wajib `accountId != toAccountId`; tanggal manual tidak boleh setelah hari ini (tanggal ke depan hanya muncul dari aturan berulang saat jatuh tempo).
3. **Arsip, bukan hapus:** akun atau kategori yang sudah punya transaksi tidak bisa dihapus, hanya diarsipkan (tetap tampil di riwayat, hilang dari pilihan baru). Akun hanya bisa diarsipkan saat saldonya Rp 0.
4. **Saldo negatif** diperbolehkan (kartu kredit/PayLater); beri penanda merah, jangan diblokir.
5. **Hapus transaksi** langsung menghapus, lalu Snackbar "Urungkan" memasukkan ulang entity yang sama (ID sama) selama beberapa detik.
6. **Periode keuangan:** tanggal mulai `startDay` 1–28 (default 1). Periode yang memuat tanggal `d` dimulai di `startDay` bulan `d` jika `d.day >= startDay`, kalau tidak di bulan sebelumnya, dan berakhir sehari sebelum `startDay` bulan berikutnya. Label periode = bulan tanggal mulainya (`startDay = 25`: "2026-11" = 25 Nov–24 Des). Beranda, anggaran, dan laporan memakai periode ini.
7. **Anggaran:** terpakai = total pengeluaran kategori itu (termasuk sub-kategorinya) dalam periode. Status: hijau < 80%, kuning 80–100%, merah > 100% (ambang bisa diubah). Anggaran total (kategori `null`) menghitung seluruh pengeluaran.
8. **Periode baru tanpa anggaran:** tawarkan "Salin dari periode lalu"; jangan menyalin diam-diam.
9. **Transaksi berulang:** saat jatuh tempo, `autoPost = true` langsung membuat transaksi, `false` mengirim pengingat untuk dikonfirmasi. Worker harus catch-up (membuat semua yang terlewat dengan tanggal jatuh tempo aslinya) dan idempotent (tidak dobel; acuannya `lastGeneratedDate`). Tanggal 29–31 di bulan yang lebih pendek dimundurkan ke hari terakhir bulan itu.
10. **Rekonsiliasi:** pengguna mengisi saldo sebenarnya; selisihnya dicatat sebagai transaksi bertanggal hari ini dengan kategori sistem "Penyesuaian saldo" (satu untuk pemasukan, satu untuk pengeluaran). Transaksi kategori sistem ini hanya memengaruhi saldo, tidak masuk laporan dan anggaran.
11. **Nominal:** pemisah ribuan otomatis saat mengetik; tampil `Rp 1.250.000` (titik ribuan, tanpa desimal; format sendiri, jangan pakai `getCurrencyInstance` mentah). Pintasan: `25rb` atau `25k` = 25.000, `1,5jt` = 1.500.000.
12. **Total saldo** di Beranda = jumlah saldo akun yang `includeInTotal` dan tidak diarsipkan.

## 5. Layar & navigasi

Bottom navigation: **Beranda · Transaksi · Anggaran · Laporan · Lainnya** (Akun, Kategori, Berulang, Backup, Pengaturan). Tombol `+` di Beranda dan Transaksi membuka form transaksi.

| Layar | Isi utama | Fase |
| --- | --- | --- |
| Onboarding | Pilih tanggal mulai periode → buat akun pertama + saldo awal → kategori bawaan | 1 |
| Beranda | Total saldo (bisa disembunyikan), pemasukan/pengeluaran/selisih periode, sisa anggaran, 5 transaksi terakhir | 1–2 |
| Form transaksi | Tab Pengeluaran/Pemasukan/Transfer, nominal besar + keypad, kategori, akun (default yang terakhir dipakai), tanggal, catatan, "Simpan & tambah lagi" | 1 |
| Daftar transaksi | Dikelompokkan per hari, swipe edit/hapus, filter & cari | 1–2 |
| Akun | Daftar + saldo, detail akun, tambah/edit/arsip, transfer, rekonsiliasi | 1 |
| Kategori | Tambah/edit/arsip/urutkan, sub-kategori | 1 |
| Anggaran | Total + per kategori, progres, salin dari periode lalu | 2 |
| Laporan | Per kategori, arus kas harian, tren 6 periode, banding periode lalu | 2 |
| Berulang & tagihan | Daftar aturan, tambah/edit/jeda | 3 |
| Backup & data | Backup/restore, ekspor/impor CSV | 4 |
| Pengaturan | Kunci aplikasi, tema, periode, pengingat, ambang anggaran | 1–4 |

Alur kunci:

1. **Catat pengeluaran:** Beranda → `+` → ketik nominal → pilih kategori (akun terakhir sudah terisi) → Simpan. Saldo dan ringkasan berubah seketika.
2. **Atur anggaran:** Anggaran → pilih kategori → isi nominal → progres muncul di Beranda.
3. **Lihat ke mana uang pergi:** Laporan → pilih periode → ketuk kategori → daftar transaksi kategori itu.

## 6. Fase implementasi

Ukuran relatif (S < M < L), bukan estimasi waktu; konversi ke minggu setelah pertanyaan terbuka #1 terjawab. Aturan kerja: satu fase satu branch, commit kecil, jangan lompat fase, centang hanya setelah tes lulus.

### Fase 0 — Setup proyek (ukuran S)

- [ ] Buat proyek Android (Kotlin, Compose, Material 3) dan repo Git; minSdk 26, Gradle Kotlin DSL + version catalog
- [ ] Pasang Hilt, Room (`exportSchema = true`), DataStore, Navigation Compose, WorkManager
- [ ] Tema Material 3 (terang/gelap/ikuti sistem) dan komponen dasar (`AmountText`, `EmptyState`)
- [ ] `Money` (format Rupiah, parser nominal), `Clock` yang bisa disuntik, `PeriodResolver`, semuanya dengan unit test
- [ ] Satu perintah untuk menjalankan `lint` + `test` (dipakai sebelum tiap commit)

**Selesai jika:** aplikasi kosong berjalan di emulator dan di HP asli, tema gelap/terang berfungsi, unit test util lulus.

### Fase 1 — Pencatatan inti, MVP (ukuran L)

- [ ] Skema Room: `Account`, `Category`, `TransactionEntity` + DAO + indeks; isi kategori bawaan (Lampiran A) saat pertama kali jalan
- [ ] Onboarding: tanggal mulai periode, akun pertama, saldo awal
- [ ] Akun: daftar dengan saldo (query `SUM`), tambah/edit/arsip, transfer antar akun, rekonsiliasi
- [ ] Kategori: tambah/edit/arsip/urutkan, satu level sub-kategori
- [ ] Form transaksi: 3 jenis, keypad nominal, akun dan kategori terakhir terisi otomatis, tanggal, catatan, "Simpan & tambah lagi"
- [ ] Daftar transaksi per hari, edit, hapus + urungkan
- [ ] Beranda v1: total saldo, ringkasan pemasukan/pengeluaran periode, 5 transaksi terakhir

**Selesai jika:**

- Pengeluaran Rp 25.000 dari akun "Dompet" mengurangi saldo Dompet tepat Rp 25.000 dan muncul di daftar hari itu.
- Transfer Rp 100.000 dari Bank ke E-wallet: saldo Bank −100.000, E-wallet +100.000, total saldo tidak berubah, dan transfer tidak masuk hitungan pemasukan/pengeluaran.
- Mengubah nominal transaksi lama memperbarui saldo seketika; hapus lalu urungkan mengembalikan data yang sama persis.
- Akun/kategori yang punya transaksi tidak bisa dihapus, hanya diarsipkan; akun hanya bisa diarsipkan saat saldonya Rp 0.
- Saldo aplikasi Rp 500.000 dan saldo sebenarnya Rp 480.000: rekonsiliasi membuat penyesuaian −Rp 20.000, saldo menjadi Rp 480.000, dan penyesuaian itu tidak dihitung sebagai pengeluaran.
- Aplikasi dimatikan paksa lalu dibuka lagi: semua data tetap ada.
- Mulai dari titik ini aplikasi sudah layak dipakai harian: mulai catat data asli.

### Fase 2 — Anggaran & laporan (ukuran L)

- [ ] Entity `Budget` + DAO (upsert menegakkan keunikan `(periodKey, categoryId)`)
- [ ] Layar Anggaran: total dan per kategori, progress bar, warna status; "Salin dari periode lalu"
- [ ] Pengaturan tanggal mulai periode (1–28); semua query memakai `PeriodResolver`
- [ ] Beranda: kartu sisa anggaran periode berjalan
- [ ] Laporan: pengeluaran per kategori (grafik + daftar), arus kas harian, tren 6 periode, banding dengan periode lalu
- [ ] Drill-down: ketuk bagian grafik → daftar transaksi terfilter
- [ ] Filter (periode, akun, kategori, jenis, rentang nominal) dan pencarian catatan di daftar transaksi

**Selesai jika:**

- Anggaran Makan & Minum Rp 1.500.000 dengan pengeluaran Rp 1.200.000 (termasuk sub-kategori) tampil 80% dan berwarna kuning; di atas 100% berwarna merah.
- Mengubah tanggal mulai periode ke 25 menggeser ringkasan, anggaran, dan laporan ke periode 25–24 tanpa mengubah data transaksi.
- Di Laporan, jumlah semua kategori = total pengeluaran periode (selisih Rp 0).
- Mengetuk satu kategori di grafik membuka daftar yang totalnya sama dengan angka di grafik.
- Mencari "kopi" menampilkan semua transaksi dengan kata itu, tanpa membedakan huruf besar/kecil.

### Fase 3 — Berulang & pengingat (ukuran M)

- [ ] Entity `RecurringRule` + layar kelola (tambah/edit/jeda/hapus)
- [ ] Worker harian (WorkManager) memproses aturan yang jatuh tempo: catch-up dan idempotent
- [ ] Mode `autoPost` (langsung jadi transaksi) dan mode konfirmasi (notifikasi → form terisi)
- [ ] Notifikasi: pengingat tagihan (H-N), anggaran ≥ 80% dan ≥ 100% (sekali per kategori per periode), pengingat harian opsional "Sudah catat hari ini?"
- [ ] Waktu pengingat "sekitar jam X" lewat WorkManager (tidak perlu izin alarm presisi); izin `POST_NOTIFICATIONS` diminta saat pengguna mengaktifkan pengingat
- [ ] App shortcut dan Quick Settings tile "Catat pengeluaran" yang langsung membuka form

**Selesai jika:**

- Aturan bulanan "Listrik" tanggal 25 menghasilkan tepat satu transaksi per bulan; bila HP mati 3 hari lalu dinyalakan, transaksinya tetap dibuat bertanggal 25 dan tidak dobel.
- Notifikasi pengingat tagihan muncul pada hari yang diatur, dan mengetuknya membuka form yang sudah terisi.
- Notifikasi anggaran muncul sekali saat melewati ambang, tidak berulang setiap aplikasi dibuka.

### Fase 4 — Keamanan & data (ukuran M)

- [ ] Kunci aplikasi: PIN + biometrik, auto-lock, sembunyikan saldo, opsi `FLAG_SECURE`
- [ ] Backup: ekspor semua data ke satu file (JSON + nomor versi skema; opsi enkripsi passphrase) lewat pemilih file Android (bisa memilih Google Drive bila tersedia di pemilih)
- [ ] Restore: validasi versi, pratinjau jumlah akun/kategori/transaksi, lalu ganti seluruh data
- [ ] Backup berkala ke folder terpilih (WorkManager) + peringatan bila lebih dari 7 hari belum backup
- [ ] Ekspor CSV (UTF-8, tanggal `yyyy-MM-dd`, nominal angka polos; pemisah koma atau titik koma, karena Excel dengan regional Indonesia kerap mengharapkan titik koma)
- [ ] Impor CSV: pemetaan kolom, pratinjau, deteksi duplikat sederhana (tanggal + nominal + akun + catatan sama)

**Selesai jika:**

- Backup lalu restore di perangkat bersih menghasilkan jumlah akun, kategori, transaksi, dan anggaran yang sama, dengan semua saldo identik.
- Aplikasi terkunci setelah waktu yang diatur di background; layar Recents tidak menampilkan isi bila `FLAG_SECURE` aktif.
- CSV hasil ekspor terbaca benar di Google Sheets dan Excel; mengimpor ulang file yang sama tidak membuat duplikat.

### Fase 5 — Poles & rilis pribadi (ukuran S)

- [ ] Aksesibilitas: `contentDescription`, urutan fokus, uji font scale 200%
- [ ] Performa: seed 5.000–10.000 transaksi, ukur cold start dan query laporan, periksa indeks
- [ ] Empty state, error state, petunjuk singkat di layar pertama
- [ ] Nama final, ikon adaptif, splash
- [ ] Build release (R8), tanda tangan APK, simpan backup keystore di tempat aman (kalau hilang, aplikasi tidak bisa diperbarui di atas versi lama)
- [ ] Pakai harian 2 minggu dengan data nyata; catat bug dan perbaiki

**Selesai jika:** target performa di bagian 7 tercapai, tidak ada crash selama 2 minggu pemakaian, dan APK release bisa dipasang di atas versi sebelumnya tanpa kehilangan data.

## 7. Non-fungsional, testing & Definition of Done

**Privasi & keamanan**

- Data hanya di perangkat. v1 tanpa izin `INTERNET` dan tanpa analytics/iklan/crash reporter pihak ketiga. Izin lain hanya `POST_NOTIFICATIONS` (Android 13+, diminta saat pengingat diaktifkan) dan biometrik.
- PIN disimpan sebagai hash (PBKDF2 + salt acak) dan percobaan salah dibatasi (mis. jeda setelah 5 kali). Auto-lock setelah 1 menit di background (bisa diatur). Tersedia opsi `FLAG_SECURE` dan sembunyikan saldo.
- File backup bisa dienkripsi dengan passphrase (AES-GCM, kunci diturunkan dari passphrase). Putuskan juga apakah Android Auto Backup dinyalakan atau dikecualikan untuk database.

**Kualitas**

- Performa: cold start < 2 detik dengan 5.000 transaksi; query laporan < 200 ms pada 10.000 transaksi; daftar dipaginasi atau dibatasi per periode, bukan memuat semua.
- Offline penuh: semua fitur v1 berfungsi tanpa internet.
- Aksesibilitas: `contentDescription`, target sentuh ≥ 48dp, kontras cukup, rapi pada font scale 200%, grafik punya alternatif berupa daftar.
- Lokalisasi: Bahasa Indonesia sebagai default, semua teks di `strings.xml`, tanggal seperti `9 Nov 2026`.
- Keandalan: operasi tulis lintas tabel dibungkus `@Transaction`; setiap perubahan skema punya tes migrasi; file backup membawa nomor versi skema.

**Tes**

- Unit: `PeriodResolver` (pergantian bulan/tahun, Februari), perhitungan saldo untuk 3 jenis transaksi, progres anggaran dengan sub-kategori, parser nominal (`25rb`, `1,5jt`, `2.500`), aturan berulang (catch-up dan idempotent), format Rupiah.
- DAO (Room in-memory): agregasi per kategori dan periode; transfer tidak masuk agregasi pengeluaran; FK RESTRICT bekerja.
- UI (Compose): alur tambah transaksi, validasi form, hapus lalu urungkan.

**Definition of Done tiap fitur:** `lint` dan `test` lulus, tidak ada string hard-coded, dicek di mode gelap, label TalkBack dasar ada, dicoba di perangkat fisik.

## 8. Backlog & risiko

**Backlog setelah v1.0**

| Ide | Catatan |
| --- | --- |
| Tujuan tabungan | Target nominal + tanggal, progres dari kontribusi atau akun terkait |
| Hutang & piutang | Siapa, jumlah, jatuh tempo, pelunasan sebagian |
| Lampiran foto struk | Simpan di penyimpanan aplikasi, dirujuk dari transaksi |
| Widget layar utama | Saldo + tombol catat |
| Pencatatan otomatis dari notifikasi bank/e-wallet | Butuh izin akses notifikasi; format tiap aplikasi berbeda dan bisa berubah. Mulai dengan eksperimen di 1–2 aplikasi yang paling sering dipakai; cek kebijakan Google Play kalau nanti dipublikasikan |
| Scan struk (OCR di perangkat) | Akurasi bervariasi; selalu minta konfirmasi sebelum menyimpan |
| Split transaksi | Satu struk, beberapa kategori |
| Multi-currency | Model uang jadi unit terkecil + kolom mata uang + kurs |
| Sinkron / berbagi dengan pasangan | Butuh backend; mengubah keputusan privasi |
| Lain-lain | Tag, laporan tahunan, ekspor PDF, pelacak langganan, nilai bersih (aset manual) |

**Risiko**

| Risiko | Mitigasi |
| --- | --- |
| Malas mencatat, data jadi bolong | Form secepat mungkin (Fase 1), shortcut/tile dan pengingat (Fase 3), mulai pakai harian sejak akhir Fase 1 |
| Kehilangan data (HP hilang/reset) | Backup berkala + peringatan 7 hari (Fase 4) |
| Bug perhitungan saldo | Saldo selalu dihitung dari transaksi, unit test, fitur rekonsiliasi |
| Migrasi database merusak data | `exportSchema`, tes migrasi, backup sebelum memasang versi dengan perubahan skema |
| Scope creep | Kerjakan berurutan per fase; ide baru masuk Backlog dulu |
| Orang lain memegang HP | Kunci aplikasi + sembunyikan saldo (Fase 4) |

## 9. Pertanyaan terbuka

1. Siapa yang membangun (kamu sendiri atau dibantu AI/Claude Code), dan berapa jam per minggu? Ini menentukan estimasi waktu per fase.
2. Periode keuangan mulai tanggal berapa (1 atau tanggal gajian)? Label periode mengikuti bulan mulai atau bulan berakhir?
3. Akun apa saja yang dipakai (kas, bank, e-wallet, kartu kredit/PayLater)? Ini menentukan tipe akun dan ikon bawaan.
4. Hutang-piutang dan tujuan tabungan: perlu di v1 atau tunggu setelah v1.0?
5. Pencatatan otomatis dari notifikasi: seberapa penting, dan aplikasi bank/e-wallet apa yang paling sering dipakai?
6. Perlu sinkron antar HP atau berbagi dengan pasangan? Kalau ya, A1 dan A3 berubah (butuh backend).
7. Nama aplikasi, dan apakah nanti akan dipublikasikan ke Play Store?
8. Backup ke file/folder sudah cukup (termasuk Drive lewat pemilih file), dan perlu enkripsi passphrase?

## Lampiran A. Kategori bawaan

**Pengeluaran** (sub-kategori setelah tanda `—`)

- Makan & Minum — Makan harian, Jajan & kopi, Belanja dapur
- Transportasi — BBM, Parkir & tol, Ojek/taksi online, Servis kendaraan
- Tagihan & Utilitas — Listrik, Air, Internet, Pulsa & paket data, Sewa/KPR
- Belanja — Pakaian, Elektronik, Rumah tangga
- Kesehatan — Obat, Dokter, Asuransi/BPJS
- Pendidikan
- Hiburan & Langganan
- Keluarga & Anak
- Sosial & Donasi — Donasi/Sedekah, Zakat, Hadiah
- Cicilan & Hutang
- Biaya admin & pajak
- Lain-lain

**Pemasukan:** Gaji, Bonus/THR, Usaha/Freelance, Investasi/Bunga, Hadiah, Penjualan, Lain-lain

**Sistem (tidak bisa dihapus, tidak masuk laporan dan anggaran):** Penyesuaian saldo (pemasukan), Penyesuaian saldo (pengeluaran)
