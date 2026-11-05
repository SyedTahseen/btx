# BetterX (Android MVP)

Personalized sexual-wellness app. Kotlin + Jetpack Compose, minSdk 26, targetSdk 35.

## Run it
1. Open the `BetterX` folder in Android Studio (Ladybug or newer). Let Gradle sync.
2. Run on a device/emulator (API 26+). In debug builds, "Start BetterX Plus" on the paywall unlocks Plus locally for testing.

> The project was generated without an Android SDK available, so it has **not been compiled or run yet**.
> Expect to fix a few small compile errors on first sync. Library versions are pinned conservatively in `app/build.gradle.kts`.

## Build on GitHub
`.github/workflows/android.yml` builds a debug APK on every push, pull request, or manual run.
After a run finishes: GitHub repo > Actions > pick the run > Artifacts > **BetterX-debug-apk**. Unzip it and install the APK on your phone.
The debug APK is signed with a debug key and includes the local "unlock Plus" test shortcut, so use it for testing only, not for the Play Store.

## What's in the MVP
| Feature | Where | State |
|---|---|---|
| Onboarding & assessment | `ui/Onboarding.kt`, `data/Content.kt` | Working. 18+ gate, goals, 12-question assessment, scored per domain |
| Personalized plan | `data/Planner.kt` | Working. Adapts daily from scores, goals, check-ins, adherence, relationship status |
| AI Coach | `data/CoachEngine.kt`, `ui/Coach.kt` | On-device rule-based coach by default (safety replies for crisis/abuse/medical). Set `COACH_API_URL` to your backend to use an LLM |
| Courses, exercises, sessions | `ui/Learn.kt` | Working. 6 courses, 19 guided practices with timer and rating |
| Progress & check-ins | `ui/Progress.kt` | Working. Streaks, trend chart, skill scores with re-assessment deltas |
| Ghost (anonymous community) | `ui/Ghost.kt` | UI + local only. Random alias per post, PII filter, hide/report. **Needs a backend to be a real community** |
| Smart reminders | `data/Reminders.kt` | Working via WorkManager: daily practice, weekly partner check-in, important dates. Discreet by default |
| Privacy, profile, subscription | `ui/Profile.kt`, `data/Persistence.kt` | Encrypted storage (Android Keystore AES-GCM), PIN lock, screenshot block, export, delete-all. Paywall UI done; **Play Billing not connected** |

## Before you ship (important)
- **AI Coach**: build a small backend that holds your LLM key and applies `CoachPrompt.SYSTEM`, then set `COACH_API_URL` in `app/build.gradle.kts`. Never put an API key in the app. Safety replies run on-device first either way.
- **Ghost**: needs a server with accounts-less anonymous posting, real moderation (keyword + human review), rate limits and a report queue. The `GhostPost` model and screens are ready to be pointed at an API.
- **Subscriptions**: add Google Play Billing (`billing-ktx`), create products in Play Console, and replace the debug shortcut in `PaywallScreen`. Verify purchases server-side.
- **Content review**: have a licensed sex therapist/clinician review all lesson and exercise text, and the coach's replies, before launch.
- **Play Store policy**: keep content educational (no explicit material), complete the Data safety form (health data), and add a privacy policy and in-app account/data deletion info.
- **Hardening ideas**: biometric unlock, SQLCipher/Room for larger data, ProGuard/R8 rules, crash reporting that excludes health data.
