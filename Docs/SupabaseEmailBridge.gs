// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

/**
 * Fotara Supabase to Gmail Webhook Bridge
 * 
 * Target recipient: arinaranetwork@gmail.com
 * Deployment: Google Apps Script Web App
 */

function doPost(e) {
  try {
    if (!e || !e.postData || !e.postData.contents) {
      return ContentService.createTextOutput(JSON.stringify({ status: "error", message: "No post data received" }))
        .setMimeType(ContentService.MimeType.JSON);
    }

    var payload = JSON.parse(e.postData.contents);
    
    // Supabase Webhook payload format: { type: "INSERT", table: "suggestions", record: { ... } }
    var record = payload.record || payload;
    
    var category = record.category || "GENERAL";
    var content = record.content || "Tidak ada pesan.";
    var senderEmail = record.email || "";
    var diagnosticInfo = record.diagnostic_info || "";
    var uuid = record.uuid || "Unknown UUID";
    var createdAt = record.created_at || new Date().toISOString();

    // Map Category to Visual Badges & Indonesian Titles
    var categoryConfig = {
      "BUG_REPORT": {
        label: "LAPORAN BUG",
        color: "#E63946",
        bgColor: "#FFE3E5",
        icon: "🐞",
        subjectPrefix: "[BUG REPORT]"
      },
      "FEATURE_IDEA": {
        label: "IDE FITUR BARU",
        color: "#D97706",
        bgColor: "#FEF3C7",
        icon: "💡",
        subjectPrefix: "[FEATURE IDEA]"
      },
      "SUGGESTION": {
        label: "SARAN & MASUKAN",
        color: "#059669",
        bgColor: "#D1FAE5",
        icon: "💬",
        subjectPrefix: "[SUGGESTION]"
      },
      "GENERAL": {
        label: "MASUKAN UMUM",
        color: "#2563EB",
        bgColor: "#DBEAFE",
        icon: "📝",
        subjectPrefix: "[FEEDBACK]"
      }
    };

    var cat = categoryConfig[category] || categoryConfig["GENERAL"];

    // Format timestamps
    var dateObj = new Date(createdAt);
    var formattedDateWIB = Utilities.formatDate(dateObj, "Asia/Jakarta", "dd MMMM yyyy, HH:mm:ss 'WIB'");
    var formattedDateUTC = Utilities.formatDate(dateObj, "UTC", "yyyy-MM-dd HH:mm:ss 'UTC'");

    // Build Email Subject
    var contentSnippet = content.replace(/\r?\n|\r/g, " ").trim();
    if (contentSnippet.length > 50) {
      contentSnippet = contentSnippet.substring(0, 47) + "...";
    }
    var emailSubject = "Fotara " + cat.subjectPrefix + ": " + contentSnippet;

    // Render HTML Email Template
    var htmlBody = `
<!DOCTYPE html>
<html lang="id">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>${emailSubject}</title>
</head>
<body style="margin: 0; padding: 0; background-color: #F1F4F9; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; color: #1E293B;">
  <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="background-color: #F1F4F9; padding: 24px 12px;">
    <tr>
      <td align="center">
        <!-- Main Card Container -->
        <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="max-width: 620px; background-color: #FFFFFF; border-radius: 14px; overflow: hidden; box-shadow: 0 4px 18px rgba(0,0,0,0.06); border: 1px solid #E2E8F0;">
          
          <!-- Header Banner -->
          <tr>
            <td style="background-color: #03071E; padding: 24px 28px; border-bottom: 3px solid ${cat.color};">
              <table role="presentation" width="100%" cellspacing="0" cellpadding="0">
                <tr>
                  <td>
                    <span style="display: inline-block; background-color: rgba(247, 127, 0, 0.15); color: #F77F00; padding: 4px 10px; border-radius: 6px; font-size: 11px; font-weight: 700; letter-spacing: 0.5px; text-transform: uppercase;">
                      Fotara System Engine
                    </span>
                    <h1 style="margin: 8px 0 0 0; color: #FFFFFF; font-size: 20px; font-weight: 700; letter-spacing: -0.3px;">
                      Masukan Pengguna Masuk
                    </h1>
                  </td>
                  <td align="right" valign="middle">
                    <span style="display: inline-block; background-color: ${cat.bgColor}; color: ${cat.color}; padding: 6px 14px; border-radius: 20px; font-size: 12px; font-weight: 700; border: 1px solid ${cat.color}; white-space: nowrap;">
                      ${cat.icon} ${cat.label}
                    </span>
                  </td>
                </tr>
              </table>
            </td>
          </tr>

          <!-- Content Body -->
          <tr>
            <td style="padding: 28px;">
              
              <!-- Message Box -->
              <div style="margin-bottom: 24px;">
                <div style="font-size: 11px; font-weight: 700; color: #64748B; text-transform: uppercase; letter-spacing: 0.8px; margin-bottom: 8px;">
                  ISI PESAN DARI PENGGUNA
                </div>
                <div style="background-color: #F8FAFC; border-left: 4px solid ${cat.color}; border-radius: 0 10px 10px 0; padding: 18px 20px; color: #0F172A; font-size: 14.5px; line-height: 1.6; white-space: pre-wrap; font-family: inherit; border-top: 1px solid #EDF2F7; border-right: 1px solid #EDF2F7; border-bottom: 1px solid #EDF2F7;">
${escapeHtml(content)}
                </div>
              </div>

              <!-- Metadata Info Grid -->
              <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="background-color: #F8FAFC; border-radius: 10px; border: 1px solid #E2E8F0; margin-bottom: 24px;">
                <tr>
                  <td style="padding: 14px 18px; border-bottom: 1px solid #E2E8F0; width: 35%; font-size: 13px; color: #64748B; font-weight: 600;">
                    Email Kontak
                  </td>
                  <td style="padding: 14px 18px; border-bottom: 1px solid #E2E8F0; font-size: 13.5px; color: #0F172A; font-weight: 600;">
                    ${senderEmail ? `<a href="mailto:${senderEmail}?subject=Tanggapan Fotara: ${encodeURIComponent(cat.label)}" style="color: #2563EB; text-decoration: none;">${escapeHtml(senderEmail)} &rarr; (Balas)</a>` : '<span style="color: #94A3B8; font-style: italic;">Anonim (Tidak Disertakan)</span>'}
                  </td>
                </tr>
                <tr>
                  <td style="padding: 14px 18px; border-bottom: 1px solid #E2E8F0; font-size: 13px; color: #64748B; font-weight: 600;">
                    Waktu Pengiriman
                  </td>
                  <td style="padding: 14px 18px; border-bottom: 1px solid #E2E8F0; font-size: 13px; color: #334155;">
                    <strong>${formattedDateWIB}</strong><br>
                    <span style="font-size: 11.5px; color: #64748B;">(${formattedDateUTC})</span>
                  </td>
                </tr>
                <tr>
                  <td style="padding: 14px 18px; border-bottom: 1px solid #E2E8F0; font-size: 13px; color: #64748B; font-weight: 600;">
                    Kategori & Status
                  </td>
                  <td style="padding: 14px 18px; border-bottom: 1px solid #E2E8F0; font-size: 13px; color: #334155;">
                    <span style="display: inline-block; font-weight: 700; color: ${cat.color};">${category}</span>
                  </td>
                </tr>
                <tr>
                  <td style="padding: 14px 18px; font-size: 13px; color: #64748B; font-weight: 600; vertical-align: top;">
                    Informasi Diagnostik
                  </td>
                  <td style="padding: 14px 18px; font-size: 12px; color: #475569; font-family: monospace;">
                    ${diagnosticInfo ? escapeHtml(diagnosticInfo) : '<span style="color: #94A3B8; font-style: italic;">Tidak ada info diagnostik</span>'}
                  </td>
                </tr>
              </table>

              <!-- Quick Action Buttons -->
              <table role="presentation" width="100%" cellspacing="0" cellpadding="0">
                <tr>
                  ${senderEmail ? `
                  <td style="padding-right: 8px;">
                    <a href="mailto:${senderEmail}?subject=Re: Fotara ${encodeURIComponent(cat.label)}" style="display: block; text-align: center; background-color: #F77F00; color: #000000; text-decoration: none; padding: 12px 18px; border-radius: 8px; font-size: 13px; font-weight: 700;">
                      Balas Pengguna
                    </a>
                  </td>
                  ` : ''}
                  <td>
                    <a href="https://supabase.com/dashboard/project/nrvnhbizyvubcdqvzabv/editor" target="_blank" style="display: block; text-align: center; background-color: #03071E; color: #FFFFFF; text-decoration: none; padding: 12px 18px; border-radius: 8px; font-size: 13px; font-weight: 600;">
                      Buka Supabase Database
                    </a>
                  </td>
                </tr>
              </table>

            </td>
          </tr>

          <!-- Footer -->
          <tr>
            <td style="background-color: #F8FAFC; padding: 18px 28px; border-top: 1px solid #E2E8F0; font-size: 11.5px; color: #94A3B8; text-align: center; line-height: 1.5;">
              Email otomatis dari database Supabase Fotara (<code style="background: #E2E8F0; padding: 2px 4px; border-radius: 4px; font-size: 11px;">public.suggestions</code>).<br>
              Hak Cipta &copy; 2026 Arinara Network. Seluruh hak cipta dilindungi.
            </td>
          </tr>

        </table>
      </td>
    </tr>
  </table>
</body>
</html>
    `;

    // Send the email directly to arinaranetwork@gmail.com
    var targetEmail = "arinaranetwork@gmail.com";
    
    MailApp.sendEmail({
      to: targetEmail,
      replyTo: senderEmail || targetEmail,
      subject: emailSubject,
      htmlBody: htmlBody
    });

    return ContentService.createTextOutput(JSON.stringify({ 
      status: "success", 
      message: "Email successfully delivered to " + targetEmail,
      category: category,
      id: record.id || null
    })).setMimeType(ContentService.MimeType.JSON);

  } catch (error) {
    return ContentService.createTextOutput(JSON.stringify({ 
      status: "error", 
      message: error.toString() 
    })).setMimeType(ContentService.MimeType.JSON);
  }
}

function escapeHtml(text) {
  if (!text) return "";
  return String(text)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#039;");
}
