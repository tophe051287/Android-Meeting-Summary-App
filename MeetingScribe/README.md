# MeetingScribe

An Android app that records a meeting, transcribes it (OpenAI Whisper), and produces
a structured recap (Anthropic Claude): summary, key points, action items with owners,
and decisions. Meetings are kept in an in-app history list; recaps can be shared or
exported as a text file.

This is a complete Android Studio project. It was written in a sandbox that has no
Android SDK/Gradle and no access to Google's or Gradle's download servers, so the
APK could not be compiled in that environment. Instead, this repo includes a GitHub
Actions workflow (`.github/workflows/build-apk.yml`) that compiles a real, installable
`.apk` in GitHub's cloud automatically — no Android Studio required. Follow the steps
below.

## 1. Get your own API keys (one-time, a few minutes)

The app calls OpenAI and Anthropic directly from your phone using keys you enter in
the app's Settings screen — no keys are hard-coded, and nothing is proxied through a
third-party server.

- OpenAI (for transcription/Whisper): create a key at platform.openai.com/api-keys
- Anthropic (for summarization/Claude): create a key at console.anthropic.com/settings/keys

Both are pay-as-you-go and cost a small fraction of a cent per minute of audio /
per summary. You don't need to enter them yet — you'll enter them after installing
the app.

## 2. Get the code into a GitHub repo

1. Create a new empty repository on github.com (public or private, either works).
2. On your computer, unzip this project, then from inside the `MeetingScribe` folder run:
   ```
   git init
   git add .
   git commit -m "Initial commit"
   git branch -M main
   git remote add origin https://github.com/<your-username>/<your-repo>.git
   git push -u origin main
   ```

## 3. Let GitHub Actions build the APK

1. Go to your repository on github.com → the **Actions** tab.
2. You should see a workflow run called "Build APK" already running (it triggers on
   every push to `main`). If it's not there, click **Actions** → **Build APK** →
   **Run workflow**.
3. Wait 3-5 minutes for it to finish (green check).
4. Click into the finished run → scroll to **Artifacts** → download
   `MeetingScribe-debug-apk`. It's a zip containing `app-debug.apk`.

## 4. Install the APK on your Android phone

1. Transfer `app-debug.apk` to your phone (email it to yourself, use a cloud drive,
   or plug in via USB).
2. Tap the file on your phone. Android will ask to allow installing from this source
   the first time — allow it, then tap Install.
3. Open MeetingScribe, grant microphone (and notification, on Android 13+) permission
   when prompted.
4. Tap the settings gear (top right) and paste in your OpenAI and Anthropic API keys.

## 5. Use it

- Tap **Record** on the home screen, then the mic button to start. Tap the stop
  button when the meeting ends.
- The app uploads the audio for transcription, then sends the transcript to Claude
  for a structured recap. This takes anywhere from a few seconds to a couple of
  minutes depending on meeting length.
- Tap a meeting in the list to see its summary, key points, action items (with
  owners), decisions, and full transcript. Use the share or export icons in the
  top bar to send the recap elsewhere.

## Notes and limitations

- Recording uses the phone's built-in microphone via a foreground service, so it
  keeps running if you lock the screen or switch apps.
- Transcription and summarization require an internet connection at the end of
  the recording (not during it — recording itself works fully offline).
- API keys are stored on-device using `EncryptedSharedPreferences` (AES-256) and
  are never written anywhere else.
- The Anthropic model used for summarization is configurable in Settings
  (defaults to `claude-3-5-sonnet-20241022`); change it if Anthropic has released
  a newer model name you'd rather use.
- This produces a debug-signed APK, which is fine for installing on your own
  device(s) but is not suitable for Play Store distribution as-is (that would
  need a release signing key, which isn't set up here).

## Project structure

```
app/src/main/java/com/ramonapps/meetingscribe/
  MainActivity.kt, MeetingScribeApp.kt
  data/            Room entities, DAO, database, repository
  network/         OpenAI Whisper + Anthropic Claude API clients (OkHttp)
  prefs/           Encrypted API key storage
  recording/       Foreground recording service + shared recording state
  ui/              Jetpack Compose screens (home, record, detail, settings) + nav graph
.github/workflows/build-apk.yml   CI that builds the APK on every push
```
