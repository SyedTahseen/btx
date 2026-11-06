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

## Accounts (email + verification, profile, change password)
Accounts use **Firebase Authentication** through its REST API (no SDK, so the app stays small).
Firebase sends the verification and reset emails itself.
Your email and a small profile (nickname, relationship, goals) are stored in Firebase.
Plans, check-ins, chats and progress stay encrypted on the device and are never uploaded.

- **Email:** sign up > verification email > user opens the link > taps "I've verified my email" > in. Sign-in is blocked until verified.
- **Forgot password:** sends a reset link from Firebase.
- **Change password:** Profile > Account > Change password (asks for the current password first).
- **Profile:** Profile > About you > edit, then **Save changes**. Saved on the device and in Cloud Firestore at `users/{uid}`; it is restored when you sign in on a new phone.
- **Delete account:** Profile > Account. Confirms with the password, deletes the online profile and the Firebase account, and clears the device.
- **Google sign-in:** removed for now. It will be added back later.

Firebase setup (one time):
1. Authentication > Sign-in method: enable **Email/Password**.
2. Build > **Firestore Database** > Create database, then Rules:
   ```
   rules_version = '2';
   service cloud.firestore {
     match /databases/{database}/documents {
       match /users/{uid} {
         allow read, write: if request.auth != null && request.auth.uid == uid;
       }
     }
   }
   ```
3. If the API key is restricted in Google Cloud, it must allow **Identity Toolkit API** and **Token Service API** (used to keep the session alive).
4. Before the Play Store: sign with your own private upload key. Declare in the Data safety form that email and profile are collected.

`app/betterx-debug.keystore` is a shared test key that keeps the signature stable across builds. It is not for production.

## Design
Modelled on a banking-app reference: soft graphite background, big animated number, light "pill" buttons,
a white bottom sheet (drag the handle or tap it to expand), a floating pill navigation bar whose selected tab grows into a white pill,
colourful program cards in yellow / mint / blue that fan out from a stack, a swipe-to-confirm control, and a full-screen green success screen.
Colours and type live in `ui/Theme.kt`; shared components in `ui/Common.kt`; motion helpers (press spring, pop-in, count-up, swipe-to-confirm, success screen) in `ui/Motion.kt`.
Screens slide in from the right and tabs cross-fade (`ui/Root.kt`). Navigation is four tabs: Today, Coach, Ghost, Progress.

## What's in the MVP
| Feature | Where | State |
|---|---|---|
| Onboarding & assessment | `ui/Onboarding.kt`, `data/Content.kt` | Working. 18+ gate, goals, 6-question assessment, scored per domain |
| Personalized plan | `data/Planner.kt` | Working. Adapts daily from scores, goals, check-ins, adherence, relationship status |
| AI Coach | `data/CoachEngine.kt`, `ui/Coach.kt` | On-device rule-based coach by default (safety replies for crisis/abuse/medical). Set `COACH_API_URL` to your backend to use an LLM |
| Courses, exercises, sessions | `ui/Learn.kt` | Working. 6 programs, 19 guided practices with timer and rating |
| Progress & check-ins | `ui/Progress.kt` | Working. Streaks, trend chart, skill scores with re-assessment deltas |
| Ghost (anonymous community) | `ui/Ghost.kt` | UI + local only. Random alias per post, PII filter, hide/report. **Needs a backend to be a real community** |
| Smart reminders | `data/Reminders.kt` | Working via WorkManager: daily practice and weekly partner check-in. Discreet by default |
| Privacy, profile, subscription | `ui/Profile.kt`, `data/Persistence.kt` | Encrypted storage (Android Keystore AES-GCM), screenshot block, editable profile synced to Firebase, change password. Paywall UI done; **Play Billing not connected** |

## Before you ship (important)
- **AI Coach**: build a small backend that holds your LLM key and applies `CoachPrompt.SYSTEM`, then set `COACH_API_URL` in `app/build.gradle.kts`. Never put an API key in the app. Safety replies run on-device first either way.
- **Ghost**: needs a server with accounts-less anonymous posting, real moderation (keyword + human review), rate limits and a report queue. The `GhostPost` model and screens are ready to be pointed at an API.
- **Subscriptions**: add Google Play Billing (`billing-ktx`), create products in Play Console, and replace the debug shortcut in `PaywallScreen`. Verify purchases server-side.
- **Content review**: have a licensed sex therapist/clinician review all lesson and exercise text, and the coach's replies, before launch.
- **Play Store policy**: keep content educational (no explicit material), complete the Data safety form (health data), and add a privacy policy and in-app account/data deletion info.
- **Hardening ideas**: biometric unlock, SQLCipher/Room for larger data, ProGuard/R8 rules, crash reporting that excludes health data.
