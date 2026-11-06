# BetterX (Android MVP)

Personalized sexual-wellness app. Kotlin + Jetpack Compose, minSdk 26, targetSdk 35.

## Run it
1. Open the `BetterX` folder in Android Studio (Ladybug or newer). Let Gradle sync.
2. Run on a device/emulator (API 26+).

> The project was generated without an Android SDK available, so it has **not been compiled or run yet**.
> Expect to fix a few small compile errors on first sync.

## Build on GitHub
`.github/workflows/android.yml` builds a minified release APK on every push, pull request, or manual run.
After a run finishes: GitHub repo > Actions > pick the run > Artifacts > **BetterX-apk**. Unzip it and install the APK on your phone.
The run log prints the APK size. The APK is signed with the debug key (fine for testing, not for the Play Store).
`TEST_UNLOCK` in `app/build.gradle.kts` lets you unlock Plus locally for testing; set it to `"false"` before publishing.

## Accounts (sign in / sign up / forgot password)
Email + password accounts use **Supabase Auth** through its REST API (no SDK, so the app stays small).
Project: **BetterX** (`gugmwcgppnczcfwreivo`, free plan, Mumbai). The URL and publishable key are built in
(`app/build.gradle.kts`); they are public by design, so no GitHub secret is needed.
Only your identity (email) is in Supabase. Health data, plans and chats stay encrypted on the device.
Email confirmation and password reset use a 6-digit code typed into the app, so no deep links are needed.

Already done on the server: project created, `delete-account` Edge Function deployed (lets users delete their own account).

**One-time step in the Supabase dashboard (about a minute):** the default emails only contain a link, so add the code.
Dashboard > BetterX > **Authentication > Email Templates**, then edit:
- **Confirm signup**: `<h2>Confirm your email</h2><p>Your BetterX code is <b>{{ .Token }}</b></p>`
- **Reset password**: `<h2>Reset your password</h2><p>Your BetterX code is <b>{{ .Token }}</b></p>`

Optional: **Authentication > Sign In / Providers > Email > Confirm email** off, if you don't want the confirmation step.
Before launch: set up **custom SMTP** (Authentication > SMTP). The built-in sender only allows a few emails per hour.

Good to know:
- Signing out keeps the local data; signing in with a *different* account on the same phone starts fresh.
- Progress is not synced between phones yet (that would need a server and consent for storing health data).
- Google sign-in is not included yet. Add Terms/Privacy links before store release.

## Design
Black background, one neon-lime accent, olive tiles, big numbers, round 1-5 buttons and a pill navigation bar.
Colors and type live in `ui/Theme.kt`; shared components (buttons, tiles, charts, keypad, pill nav) in `ui/Common.kt`.
Navigation is four tabs: Today, Coach, Ghost, Progress. Programs, check-in and settings open from Today.

## What's in the MVP
| Feature | Where | State |
|---|---|---|
| Onboarding & assessment | `ui/Onboarding.kt`, `data/Content.kt` | Working. 18+ gate, goals, 6-question assessment, scored per domain |
| Personalized plan | `data/Planner.kt` | Working. Adapts daily from scores, goals, check-ins, adherence, relationship status |
| AI Coach | `data/CoachEngine.kt`, `ui/Coach.kt` | On-device rule-based coach by default (safety replies for crisis/abuse/medical). Set `COACH_API_URL` to your backend to use an LLM |
| Courses, exercises, sessions | `ui/Learn.kt` | Working. 6 programs, 19 guided practices with timer and rating |
| Progress & check-ins | `ui/Progress.kt` | Working. Streaks, trend chart, skill scores with re-assessment deltas |
| Ghost (anonymous community) | `ui/Ghost.kt` | UI + local only. Random alias per post, PII filter, hide/report. **Needs a backend to be a real community** |
| Smart reminders | `data/Reminders.kt` | Working via WorkManager: daily practice, weekly partner check-in, important dates. Discreet by default |
| Privacy, profile, subscription | `ui/Profile.kt`, `data/Persistence.kt` | Encrypted storage (Android Keystore AES-GCM), screenshot block, export, delete-all. Paywall UI done; **Play Billing not connected** |

## Before you ship (important)
- **AI Coach**: build a small backend that holds your LLM key and applies `CoachPrompt.SYSTEM`, then set `COACH_API_URL` in `app/build.gradle.kts`. Never put an API key in the app. Safety replies run on-device first either way.
- **Ghost**: needs a server with accounts-less anonymous posting, real moderation (keyword + human review), rate limits and a report queue. The `GhostPost` model and screens are ready to be pointed at an API.
- **Subscriptions**: add Google Play Billing (`billing-ktx`), create products in Play Console, and replace the debug shortcut in `PaywallScreen`. Verify purchases server-side.
- **Content review**: have a licensed sex therapist/clinician review all lesson and exercise text, and the coach's replies, before launch.
- **Play Store policy**: keep content educational (no explicit material), complete the Data safety form (health data), and add a privacy policy and in-app account/data deletion info.
- **Hardening ideas**: biometric unlock, SQLCipher/Room for larger data, ProGuard/R8 rules, crash reporting that excludes health data.
