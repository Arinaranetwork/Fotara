<!-- --- Arinara Network (c) 2026 --- -->
<!-- Exclusive property of Arinara Network. -->
<!-- Unauthorized use, reproduction, distribution, or modification of this code, -->
<!-- in whole or in part, for any purpose, is strictly prohibited without prior -->
<!-- written consent from Arinara Network as sole legal owner of this codebase. -->

# Feedback Backend & Client Verification Checklist (C11)

This document specifies the end-to-end verification test plan and automated script for validating the Supabase feedback backend and client-side `FeedbackManager` during the final validation chunk (C11).

---

## 1. Prerequisites for C11 Verification
1. Supabase database initialized or updated with [`Docs/Supabase_Schema.sql`](file:///c:/Users/sepli/OneDrive/Documents/File%20DD/Coding/Fotara/Docs/Supabase_Schema.sql) or [`Docs/Migration_2026-10-01_Feedback_Limits_And_Categories.sql`](file:///c:/Users/sepli/OneDrive/Documents/File%20DD/Coding/Fotara/Docs/Migration_2026-10-01_Feedback_Limits_And_Categories.sql).
2. Mobile device or emulator connected to internet.
3. Verification script: [`scratch/verify_feedback_backend.mjs`](file:///c:/Users/sepli/OneDrive/Documents/File%20DD/Coding/Fotara/scratch/verify_feedback_backend.mjs) (Node.js test runner).

---

## 2. End-to-End Test Matrix

| Test ID | Test Case | Input Parameters | Expected Result | Pass / Fail |
| :--- | :--- | :--- | :--- | :--- |
| **FB-01** | Category: Bug Report | Category = "Bug Report", valid content, email provided, diagnostics enabled | HTTP 201 / 200 via PostgREST, `synced = true`, confirmation toast, email webhook dispatched | [ ] |
| **FB-02** | Category: Suggestion | Category = "Suggestion", valid content, email empty, diagnostics disabled | HTTP 201 / 200, row inserted without email or diagnostic string | [ ] |
| **FB-03** | Category: Feature Idea | Category = "Feature Idea", valid content | HTTP 201 / 200, correct category tag displayed in Supabase | [ ] |
| **FB-04** | Category: General | Category = "General", short message | HTTP 201 / 200, General category assigned | [ ] |
| **FB-05** | Long Message (Boundary) | Content length = 4,990 characters | Successfully stored without truncation or PostgREST length errors | [ ] |
| **FB-06** | Special Characters | Quotes, slashes, brackets: `Test "quote" 'single' <xml> & / \ {json: true}` | Properly escaped JSON payload, accepted by PostgREST and stored verbatim | [ ] |
| **FB-07** | Non-ASCII / Math Symbols | Unicode symbols: `Integral ∫ f(x)dx, Greek α + β = γ, Diacritics: crème fraîche` | UTF-8 encoded, byte length calculated correctly, stored intact | [ ] |
| **FB-08** | Empty Optional Fields | `email = ""`, `includeDiagnostics = false` | Payload omits empty keys, PostgREST fills defaults, no null-pointer crashes | [ ] |
| **FB-09** | 60-Second Cooldown | Submit two feedbacks within 60 seconds | First succeeds; second blocked by client with countdown "Wait (58s)". If forced to server, DB trigger raises P0001 | [ ] |
| **FB-10** | 5 Submissions Daily Quota | Submit 5 feedbacks within rolling 24-hour window | 5 succeed; 6th attempt blocked with "Daily submission limit reached (5 per 24 hours). Next slot opens in Xh Ym" | [ ] |
| **FB-11** | Offline Queue Persistence | Turn Airplane mode ON -> Submit feedback | Dialog indicates feedback saved locally. Network returns ON -> feedback flushes cleanly one at a time | [ ] |
| **FB-12** | Queue Poison Prevention | Queue malformed payload (e.g., bad format) | HTTP 400 client format error received; item dropped from queue to prevent infinite retry loop | [ ] |

---

## 3. Node.js Verification Runner (`scratch/verify_feedback_backend.mjs`)

Execute the following automated runner during C11 to validate the live Supabase PostgREST endpoint:

```bash
node scratch/verify_feedback_backend.mjs
```

Script capabilities:
- Tests all 4 category check constraints.
- Tests special characters and Unicode preservation.
- Tests rapid submission to verify server trigger `P0001` rate limit behavior.
- Verifies that `Prefer: return=minimal` is accepted without RLS read violations.
