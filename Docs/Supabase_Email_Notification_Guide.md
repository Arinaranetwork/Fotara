<!-- --- Arinara Network (c) 2026 --- -->
<!-- Exclusive property of Arinara Network. -->
<!-- Unauthorized use, reproduction, distribution, or modification of this code, -->
<!-- in whole or in part, for any purpose, is strictly prohibited without prior -->
<!-- written consent from Arinara Network as sole legal owner of this codebase. -->

# Supabase Automated Email Notification Guide to arinaranetwork@gmail.com

This document describes the architecture and configuration procedures to ensure every user submission (suggestions, feature ideas, bug reports) inserted into the `public.suggestions` table in Supabase is immediately dispatched to **`arinaranetwork@gmail.com`** in a clean, modern, and professional HTML format.

---

## 1. Solution Architecture

Supabase does not supply a custom SMTP dispatch server for arbitrary outbound emails (its native email service is restricted to Auth flows such as confirmation and password resets). Therefore, notifications to the developer inbox utilize the **Supabase Database Webhooks** mechanism.

Two fully supported deployment methods are available:

| Feature | Method 1: Google Apps Script Webhook (Recommended) | Method 2: Supabase Edge Function / Resend API |
| :--- | :--- | :--- |
| **Cost** | **100% Free Indefinitely** | Free tier up to volume limits (e.g. 100 emails/day) |
| **Setup Time** | **2 Minutes (No credit card or API key needed)** | Requires 3rd-party registration (Resend/Sendgrid) |
| **Deliverability** | **100% Gmail Inbox Delivery** (Dispatched via Google) | Dependent on domain reputation and DKIM/SPF setup |
| **Source File** | [`Docs/SupabaseEmailBridge.gs`](file:///c:/Users/sepli/OneDrive/Documents/File%20DD/Coding/Fotara/Docs/SupabaseEmailBridge.gs) | [`Docs/Supabase_Schema.sql`](file:///c:/Users/sepli/OneDrive/Documents/File%20DD/Coding/Fotara/Docs/Supabase_Schema.sql) |

---

## 2. Method 1: Google Apps Script Setup (2 Minutes, Free)

This method employs Google Apps Script associated directly with your Google account (`arinaranetwork@gmail.com`). Each time a new row is inserted in Supabase, Supabase calls the Google Apps Script Webhook URL, which immediately formats and delivers a rich HTML email to your inbox.

### Step 0: Create `public.suggestions` Table in Supabase (Prerequisite)
If the `public.suggestions` table has not yet been created:
1. Open the Supabase SQL Editor:
   `https://supabase.com/dashboard/project/nrvnhbizyvubcdqvzabv/sql/new`
2. Open [`Docs/Supabase_Schema.sql`](file:///c:/Users/sepli/OneDrive/Documents/File%20DD/Coding/Fotara/Docs/Supabase_Schema.sql) in this repository and copy the script.
3. Paste it into the Supabase SQL Editor.
4. Click **Run** (or press `Ctrl+Enter`).
5. The `public.suggestions` table is now ready to receive data and webhooks.

### Step 1: Create the Webhook in Google Apps Script
1. Open your browser and log into **`arinaranetwork@gmail.com`**.
2. Visit [script.google.com](https://script.google.com/).
3. Click **New project** in the top left.
4. Name the project: `Fotara Feedback Notifier`.
5. Replace all code in `Code.gs` with the entire contents of:
   [`Docs/SupabaseEmailBridge.gs`](file:///c:/Users/sepli/OneDrive/Documents/File%20DD/Coding/Fotara/Docs/SupabaseEmailBridge.gs)
6. Save the project (`Ctrl+S`).

### Step 2: Deploy as a Web App
1. Click **Deploy** in the top right &rarr; select **New deployment**.
2. Click the gear icon (Select type) &rarr; select **Web app**.
3. Configure the deployment:
   - **Description**: `Fotara Supabase Webhook Bridge v1`
   - **Execute as**: `Me (arinaranetwork@gmail.com)`
   - **Who has access**: **`Anyone`** *(Required so Supabase servers can post payloads without browser authentication)*.
4. Click **Deploy**.
5. Grant permissions (*Authorize Access*), choose `arinaranetwork@gmail.com`, click **Advanced** &rarr; **Go to Fotara Feedback Notifier (unsafe)**, and click **Allow**.
6. Copy the **Web app URL** (`https://script.google.com/macros/s/AKfycb.../exec`).

### Step 3: Register the Webhook in the Supabase Dashboard
1. Open your Supabase project dashboard:
   `https://supabase.com/dashboard/project/nrvnhbizyvubcdqvzabv/database/hooks`
2. In the left navigation, select **Database** &rarr; **Webhooks**.
3. Click **Create a webhook**.
4. Configure fields:
   - **Name**: `notify_feedback_email`
   - **Table**: `public.suggestions`
   - **Events**: Check only **`Insert`** (uncheck Update and Delete).
   - **Type of webhook**: `HTTP Request`
   - **HTTP method**: `POST`
   - **URL**: Paste the Web app URL obtained in Step 2.
   - **HTTP Headers**:
     - Key: `Content-Type`, Value: `application/json`
5. Click **Create webhook**.

---

## 3. Received Email Visual Hierarchy

Emails delivered to `arinaranetwork@gmail.com` feature clean, high-contrast layouts:

- **Structured Subjects**:
  - `Fotara [BUG REPORT]: App freezes when scanning document...`
  - `Fotara [FEATURE IDEA]: Add PDF export option...`
  - `Fotara [SUGGESTION]: Folder color suggestion...`
- **Category Badges**:
  - **BUG REPORT**: Crimson `#E63946` with soft pink background.
  - **FEATURE IDEA**: Amber Gold `#D97706` with soft yellow background.
  - **SUGGESTION**: Emerald Teal `#059669` with soft green background.
  - **GENERAL FEEDBACK**: Electric Blue `#2563EB` with soft blue background.
- **User Message Container**:
  - Message body enclosed in a high-contrast container with category-colored left border.
- **Metadata Details**:
  - **Contact Email**: One-tap interactive `Reply to User` link via Gmail, or labeled *Anonymous*.
  - **Timestamp**: Converted to UTC.
  - **Diagnostic Info**: Device model, Android OS version, and app version when included.
- **Action Shortcuts**:
  - Direct button to navigate to the Supabase database editor.

---

## 4. Webhook Test Verification

To verify that the dispatch pipeline functions without sending from a physical device:

1. Open **SQL Editor** in Supabase:
   `https://supabase.com/dashboard/project/nrvnhbizyvubcdqvzabv/sql/new`
2. Execute the following test query:

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
    'Automated test submission for the Fotara feedback notification system. Delivery to arinaranetwork@gmail.com with the BUG REPORT badge confirms active webhook integration.',
    'test.user@example.com',
    'Device: Google Pixel 8 Pro | Android: 14 (API 34) | App: v1.5.0 (Build 13)',
    EXTRACT(EPOCH FROM NOW())::BIGINT * 1000
);
```

3. Open Gmail at **`arinaranetwork@gmail.com`**. Within 3-5 seconds, the formatted notification will arrive in your inbox.
4. You may remove the test record from the `suggestions` table at any time.

---

## 5. Security, Rate Limits & Privacy Summary

1. **Insert-Only Permissions**: Client applications connect with `anon` credentials restricted strictly to `INSERT` operations with `Prefer: return=minimal`. Anonymous clients possess zero `SELECT` permissions.
2. **Tightened Rate Limits**: Enforced on both client and database trigger:
   - **Cooldown**: 60 seconds between submissions per installation UUID.
   - **Daily Quota**: Maximum of 5 submissions per rolling 24-hour window per installation UUID.
   - **Database Trigger Exception**: Returns error code `P0001` allowing the mobile client to distinguish rate-limit pauses from malformed requests.
3. **Database Migration Script**: To update existing live Supabase instances to the new constraints and trigger, run [`Docs/Migration_2026-10-01_Feedback_Limits_And_Categories.sql`](file:///c:/Users/sepli/OneDrive/Documents/File%20DD/Coding/Fotara/Docs/Migration_2026-10-01_Feedback_Limits_And_Categories.sql).
4. **Data Confidentiality**: Client `anon` roles possess no `SELECT` permissions on `public.suggestions`, preventing inspection of other users' submissions.
5. **Protected Dispatch**: Webhook operations execute server-to-server, with no developer credentials or email secrets embedded in client application binaries.
