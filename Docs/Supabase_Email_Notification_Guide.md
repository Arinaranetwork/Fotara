<!-- --- Arinara Network (c) 2026 ---
Exclusive property of Arinara Network.
Unauthorized use, reproduction, distribution, or modification of this code,
in whole or in part, for any purpose, is strictly prohibited without prior
written consent from Arinara Network as sole legal owner of this codebase. -->

# Panduan Integrasi Email Otomatis Supabase ke arinaranetwork@gmail.com

Dokumen ini menjelaskan arsitektur dan langkah konfigurasi agar setiap masukan (*suggestion*, ide fitur, laporan bug) yang masuk ke tabel `public.suggestions` di Supabase langsung diteruskan ke email **`arinaranetwork@gmail.com`** dengan format HTML yang rapi, modern, dan profesional.

---

## 1. Arsitektur Solusi

Supabase tidak menyediakan server pengiriman SMTP kustom untuk email publik (layanan bawaan Supabase hanya untuk Auth seperti konfirmasi akun/reset kata sandi). Oleh karena itu, notifikasi ke inbox pengembang menggunakan mekanisme **Supabase Database Webhook**.

Terdapat dua metode yang didukung penuh:

| Fitur | Metode 1: Google Apps Script Webhook (Rekomendasi Utama) | Metode 2: Supabase Edge Function / Resend API |
| :--- | :--- | :--- |
| **Biaya** | **100% Gratis Selamanya** | Gratis hingga kuota tertentu (e.g. 100 email/hari) |
| **Setup** | **2 Menit (Tanpa kartu kredit/API key)** | Perlu registrasi akun pihak ke-3 (Resend/Sendgrid) |
| **Deliverability** | **100% Masuk Inbox Gmail** (Dikirim via akun Google) | Tergantung reputasi domain pengirim |
| **File Sumber** | [`Docs/SupabaseEmailBridge.gs`](file:///c:/Users/sepli/OneDrive/Documents/File%20DD/Coding/Fotara/Docs/SupabaseEmailBridge.gs) | [`Docs/Supabase_Schema.sql`](file:///c:/Users/sepli/OneDrive/Documents/File%20DD/Coding/Fotara/Docs/Supabase_Schema.sql) |

---

## 2. Metode 1: Setup Google Apps Script (2 Menit, 100% Gratis)

Metode ini menggunakan Google Apps Script yang terpasang langsung pada akun Google Anda (`arinaranetwork@gmail.com`). Setiap kali ada baris baru di Supabase, Supabase memanggil webhook URL Google Apps Script, dan script langsung mengirimkan email format HTML cantik ke inbox Anda.

### Langkah 0: Buat Tabel `public.suggestions` di Supabase (Prasyarat Wajib)
Jika tabel `public.suggestions` belum muncul di dashboard atau dropdown Webhooks Anda, buat tabel terlebih dahulu dalam 30 detik:
1. Buka Supabase SQL Editor:
   `https://supabase.com/dashboard/project/nrvnhbizyvubcdqvzabv/sql/new`
2. Buka file [`Docs/Supabase_Schema.sql`](file:///c:/Users/sepli/OneDrive/Documents/File%20DD/Coding/Fotara/Docs/Supabase_Schema.sql) di project ini, lalu salin seluruh kodenya.
3. Tempelkan (*paste*) ke SQL Editor di Supabase.
4. Klik tombol **Run** (atau tekan `Ctrl+Enter`).
5. Selesai! Pesan `Success. No rows returned` akan muncul. Sekarang tabel `public.suggestions` sudah resmi aktif dan siap menerima data maupun webhook.

### Langkah 1: Buat Webhook di Google Apps Script
1. Buka browser dan login ke akun **`arinaranetwork@gmail.com`**.
2. Kunjungi: [script.google.com](https://script.google.com/).
3. Klik tombol **New project** (Proyek Baru) di pojok kiri atas.
4. Beri nama proyek: `Fotara Feedback Notifier`.
5. Hapus semua teks di editor `Code.gs`, lalu salin seluruh isi dari file:
   [`Docs/SupabaseEmailBridge.gs`](file:///c:/Users/sepli/OneDrive/Documents/File%20DD/Coding/Fotara/Docs/SupabaseEmailBridge.gs)
6. Tempelkan ke editor Google Apps Script, lalu klik ikon **Save** (Ctrl+S / Cmd+S).

### Langkah 2: Deploy sebagai Web App
1. Klik tombol **Deploy** di kanan atas &rarr; pilih **New deployment**.
2. Klik ikon gerigi (Select type) di samping kiri &rarr; pilih **Web app**.
3. Isi konfigurasi berikut:
   - **Description**: `Fotara Supabase Webhook Bridge v1`
   - **Execute as**: `Me (arinaranetwork@gmail.com)`
   - **Who has access**: **`Anyone`** *(PENTING: Pilih "Anyone" agar server Supabase dapat mengirimkan POST data tanpa autentikasi browser Google)*.
4. Klik tombol **Deploy**.
5. Google akan meminta izin (*Authorize access*). Klik **Authorize Access**, pilih akun `arinaranetwork@gmail.com`, klik **Advanced** &rarr; klik **Go to Fotara Feedback Notifier (unsafe)**, lalu klik **Allow**.
6. Salin **Web app URL** yang muncul (formatnya: `https://script.google.com/macros/s/AKfycb.../exec`).

### Langkah 3: Pasang Webhook di Dashboard Supabase
1. Buka dashboard proyek Supabase Anda:
   `https://supabase.com/dashboard/project/nrvnhbizyvubcdqvzabv/database/hooks`
2. Di menu kiri, pilih **Database** &rarr; **Webhooks**.
3. Klik tombol **Create a webhook** (atau *Enable Webhooks* jika belum aktif).
4. Masukkan data:
   - **Name**: `notify_feedback_email`
   - **Table**: `public.suggestions`
   - **Events**: Centang hanya **`Insert`** (uncheck Update dan Delete).
   - **Type of webhook**: `HTTP Request`
   - **HTTP method**: `POST`
   - **URL**: Tempelkan *Web app URL* dari Langkah 2 di atas.
   - **HTTP Headers**:
     - Key: `Content-Type`, Value: `application/json`
5. Klik **Create webhook**.

---

## 3. Tampilan & Desain Email yang Diterima

Email yang diterima di `arinaranetwork@gmail.com` telah dirancang secara premium dengan visual elegan:

- **Subjek Terstruktur**:
  - `Fotara [BUG REPORT]: Aplikasi freeze saat scan dokumen...`
  - `Fotara [FEATURE IDEA]: Tambahkan fitur export PDF...`
  - `Fotara [SUGGESTION]: Saran warna folder...`
- **Badge Kategori Warna**:
  - 🐞 **LAPORAN BUG**: Merah `#E63946` dengan latar merah muda lembut.
  - 💡 **IDE FITUR BARU**: Amber Gold `#D97706` dengan latar kuning lembut.
  - 💬 **SARAN & MASUKAN**: Emerald Teal `#059669` dengan latar hijau lembut.
  - 📝 **MASUKAN UMUM**: Electric Blue `#2563EB` dengan latar biru lembut.
- **Kartu Pesan Pengguna**:
  - Teks pesan pengguna ditaruh di dalam wadah kutipan bergaris tepi warna kategori dengan tipografi nyaman dibaca.
- **Rincian Metadata**:
  - **Email Kontak**: Link interaktif satu ketuk `Balas Pengguna` langsung via Gmail. Jika tidak diisi, otomatis berlabel *Anonim*.
  - **Waktu**: Dikonversi ke waktu lokal WIB (`Asia/Jakarta`) dan UTC.
  - **Info Diagnostik**: Menampilkan model ponsel, versi Android, dan versi aplikasi jika disertakan oleh pengguna.
- **Tombol Pintas**:
  - Tombol langsung membuka tabel Supabase di browser untuk moderasi instan.

---

## 4. Pengujian Webhook (Test Verification)

Untuk memverifikasi bahwa alur pengiriman berjalan sempurna tanpa harus mengirim dari ponsel:

1. Buka **SQL Editor** di Supabase:
   `https://supabase.com/dashboard/project/nrvnhbizyvubcdqvzabv/sql/new`
2. Jalankan query pengujian berikut:

```sql
INSERT INTO public.suggestions (
    uuid,
    category,
    content,
    email,
    diagnostic_info,
    submitted_at
) VALUES (
    'test-uuid-verification',
    'BUG_REPORT',
    'Halo tim Arinara, ini adalah email uji coba otomatis sistem feedback Fotara. Jika Anda membaca pesan ini di arinaranetwork@gmail.com dengan badge merah BUG REPORT, berarti integrasi webhook database telah 100% BERHASIL dan SIAP PRODUKSI!',
    'pengguna.test@example.com',
    'Device: Google Pixel 8 Pro | Android: 14 (API 34) | App: v1.3.4 (Build 4)',
    EXTRACT(EPOCH FROM NOW())::BIGINT * 1000
);
```

3. Buka Gmail di **`arinaranetwork@gmail.com`**. Dalam 3-5 detik, notifikasi email berformat rapi akan masuk ke inbox Anda!
4. Anda dapat menghapus data pengujian tersebut kapan saja dari tabel `suggestions`.

---

## 5. Metode Alternatif 2: SQL Trigger Langsung via `pg_net`

Jika Anda lebih memilih seluruh konfigurasi berada di dalam skrip SQL database tanpa menggunakan menu Webhook UI Supabase:

```sql
-- 1. Aktifkan ekstensi pg_net
CREATE EXTENSION IF NOT EXISTS pg_net;

-- 2. Buat fungsi pemanggil webhook
CREATE OR REPLACE FUNCTION public.forward_feedback_to_email()
RETURNS TRIGGER AS $$
DECLARE
    webhook_url TEXT := 'TEMPELKAN_WEBAPP_URL_GOOGLE_APPS_SCRIPT_DISINI';
BEGIN
    PERFORM net.http_post(
        url := webhook_url,
        body := json_build_object(
            'type', TG_OP,
            'table', TG_TABLE_NAME,
            'schema', TG_TABLE_SCHEMA,
            'record', row_to_json(NEW)
        )::jsonb,
        headers := '{"Content-Type": "application/json"}'::jsonb
    );
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 3. Pasang trigger setelah insert
DROP TRIGGER IF EXISTS trg_forward_feedback_email ON public.suggestions;

CREATE TRIGGER trg_forward_feedback_email
AFTER INSERT ON public.suggestions
FOR EACH ROW
EXECUTE FUNCTION public.forward_feedback_to_email();
```

---

## 6. Ringkasan Keamanan & Privasi

1. **Anon Insert-Only**: Pengguna aplikasi Android hanya memiliki hak `INSERT` dengan kuota 10 pengiriman per jam.
2. **Kerahasiaan Data**: Klien umum (`anon`) tidak memiliki izin `SELECT` sehingga tidak dapat membaca masukan pengguna lain.
3. **Penyampaian Aman**: Webhook berjalan di sisi server (*server-to-server*), tidak melibatkan eksposur email tujuan atau token rahasia di dalam binary APK ponsel.
