Privacy Policy
Version: 1 | Effective: 2026-10-06

## Introduction
Fotara is designed from the ground up as an offline-first study and coursework notebook application for students, researchers, and learners. Your educational materials are private, and Fotara does not require an account, does not serve advertisements, does not track you across other apps, and does not sell your data.

## What Stays on Your Device
Everything you create and store inside Fotara stays on your local device storage by default:
- **Coursework and Notes**: All subject folders, subfolder structures, rich text notes, markdown documents, and word documents.
- **Photos and Scans**: Photos captured with your device camera or imported from your photo library.
- **Drawings and Annotations**: Freehand vector strokes, highlighters, eraser marks, and multi-layer drawing canvases.
- **PDF Documents**: PDF course materials, imported slides, and page bookmarks.
- **User Profile**: Your custom profile name, optional local email label, chosen avatar image, and banner graphics.
- **Settings and Preferences**: Display themes, grid density, OCR configurations, study reminder schedules, and recent search history.
- **Local Crash Logs**: Uncaught application exceptions are written locally to an internal file (`crash.log`) within Fotara's private directory. Crash logs remain on your device and are never automatically transmitted over the internet.

## What Leaves Your Device and When
Fotara performs network communication only for specific, transparent features:

### 1. App Update Checks and Release Assets
- **Host**: `api.github.com`, `github.com`, and `*.githubusercontent.com`.
- **Purpose**: Checking whether a newer version of Fotara is available, displaying release notes and release banner images, and downloading updated APK installation packages.
- **Trigger**: Performed automatically on app launch (if enabled in Settings) or when you tap "Check for Updates" in Settings.
- **Transmitted Data**: Standard HTTP GET requests. No user account, device identifier, or personal data is transmitted.

### 2. User Feedback Submissions
- **Host**: `nrvnhbizyvubcdqvzabv.supabase.co`.
- **Purpose**: Allowing students to submit bug reports, feature suggestions, and general feedback directly to the developer.
- **Trigger**: Transmitted only when you explicitly open the Feedback dialog and tap "Submit Feedback" (or during subsequent automatic retries if submitted while offline).
- **Transmitted Data**:
  - A unique random feedback identifier.
  - A random installation UUID stored locally in app preferences.
  - Your chosen category (such as "Suggestion" or "Bug Report").
  - The text content you typed in the feedback field.
  - Your email address (completely optional; included only if you type it in).
  - Diagnostic hardware and software details (optional; included only if the diagnostic checkbox is selected): Fotara version, Android OS version and SDK level, and device manufacturer and model.
  - Timestamp of submission.

### 3. Optional Anonymous Device Count
- **Host**: `nrvnhbizyvubcdqvzabv.supabase.co`.
- **Purpose**: Helping the developer estimate the approximate number of active devices running Fotara.
- **Trigger**: Occurs only after you accept these terms (if enabled), at most once every 24 hours, or immediately when the app version changes.
- **Transmitted Data**:
  - `device_id`: A randomly generated UUID v4 created locally on first use. It is not tied to your Google account, Android ID, advertising ID, IMEI, MAC address, hardware serial number, IP address, or device model.
  - `app_version`: The numerical app version string (for example, `1.8.0`).
  - `channel`: The release channel (`stable` or `beta`).
- **Control and Deletion**: You can disable the anonymous device count at first launch or at any time in Settings > About & Legal > Privacy. Turning this setting off immediately deletes your local device ID and queues a request to remove the ID from the server database.

### 4. Server Logs and IP Addresses
When connecting to GitHub or Supabase, standard internet protocol routing requires transmission of your IP address. These third-party hosting infrastructure providers may process and record IP addresses in standard server access logs according to their own system operations.

## Permissions and Why They Are Needed
Fotara requests only the permissions necessary to provide its offline educational features:
- **Camera (`CAMERA`)**: Allows you to take photos of whiteboards, textbooks, and lecture slides directly into coursework folders.
- **Photos / Media (`READ_MEDIA_IMAGES` / `READ_EXTERNAL_STORAGE`)**: Allows you to import coursework photos and PDF documents from your device gallery.
- **Notifications (`POST_NOTIFICATIONS`)**: Displays study deadline reminders and alerts you when a new update download completes.
- **Exact Alarms (`SCHEDULE_EXACT_ALARM`, `USE_EXACT_ALARM`)**: Delivers precise study reminders and exam countdown alerts on time, even when the device is in Doze mode.
- **Run at Startup (`RECEIVE_BOOT_COMPLETED`)**: Reschedules your active study reminders after your device is restarted.
- **Full Screen Intent (`USE_FULL_SCREEN_INTENT`)**: Shows high-priority alerts for critical study deadlines.
- **Vibration (`VIBRATE`)**: Provides haptic feedback when organizing coursework tabs and sounding alarms.
- **Install Packages (`REQUEST_INSTALL_PACKAGES`)**: Prompts you to install updated Fotara APK packages after downloading them from GitHub releases.
- **Internet (`INTERNET`)**: Enables checking GitHub for updates, downloading APKs, sending optional user feedback, and sending the optional anonymous device count.

## Children and Students
Fotara is designed for students. The app does not require account creation, does not ask for personal details, does not collect names or birthdates, and does not profile users. The optional device count uses only a random UUID without personal identifiers.

## Changes to this Policy
If this Privacy Policy is updated, the new version will be bundled with the application update, and you will be prompted to review and accept the revised policy before continuing to use Fotara.

## Contact
You can contact the developer through:
- **GitHub Issues**: https://github.com/Arinaranetwork/Fotara/issues
- **In-App Feedback**: Settings > Send Feedback
