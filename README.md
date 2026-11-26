# YT

A lightweight YouTube and YouTube Music client with a modern Liquid Glass UI, ad-free playback, endless personalised Shorts, synced Apple Music-style lyrics, and high-quality downloads.

## Features

- Liquid Glass interface across the player, Shorts, fullscreen controls, settings and music mini player
- Ad-free YouTube and YouTube Music playback
- Personalised Shorts feed with a one-tap fresh batch and loading animation
- Synced lyrics with an Apple Music-style animated lyric view
- High-quality video and audio downloads up to 4K / HDR
- 4K HDR playback and customizable fixed resolution quality lock
- Search, channels, playlists and community posts
- Background playback and picture-in-picture
- Android TV support too: the same APK runs on TV with full D-pad navigation

## Build & release (GitHub Actions)

1. Push this project to your GitHub repo.
2. Go to **Actions → Build & Release → Run workflow**, type a version (e.g. `2.3.0`) and what's new, and run it.
3. The APK (`YT.apk`) appears under **Releases**. Installed apps check your repo's latest release and offer the update inside the app.

## Signing (important)

The signing key is currently stored in the repo (`app/release.keystore`, default password). Anyone who can read the repo can sign an APK that your installed app would accept as an update. To fix this **without breaking updates**:

1. On your computer run `base64 -w0 app/release.keystore` and copy the output.
2. GitHub repo → Settings → Secrets and variables → Actions → add `KEYSTORE_BASE64` (that output), plus `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` (current values: `android`, `androiddebugkey`, `android`).
3. Run the workflow once and confirm the warning about the repo keystore is gone.
4. Then run `git rm --cached app/release.keystore` and commit. The builds keep the same signature, so installed apps still update.

For real protection create your own private key later (`keytool -genkeypair ...`); that changes the signature, so everyone has to uninstall and reinstall once.

Developer: [@alive_harsh_](https://www.instagram.com/alive_harsh_) · Based on the open-source YTXTube project (GPL-3.0).

## Base [FlashTube](https://github.com/JustaThinker/FlashTube)

## Build

```bash
./gradlew :app:assembleRelease
```

The release APK is written to `app/build/outputs/apk/release/app-release.apk`.

Licensed under GPLv3. See [LICENSE](LICENSE).
