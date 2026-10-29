# Abyss Files + Abyss Home

Two tiny ad-free apps for an Android TV install on a Nintendo Switch (Switchroot):

- **Abyss Files** is the file manager (below).
- **Abyss Home** is the launcher / home screen: clock, big tiles for Jellyfin, Kodi, Plex, Files and Settings,
  and a row with all your installed apps. Install it, then press Home and choose **Abyss Home** ("Always").

## Abyss Files

A tiny, ad-free file manager for Android TV (made for a Switchroot / LineageOS Android TV install on a
Nintendo Switch). Dark "Abyss"-inspired look, built for a controller or remote: everything is reachable with
the D-pad, A/OK to open, long-press A/OK to select, B/Back to go up.

Features: browse internal storage and SD/USB, Downloads/Movies/Music/Pictures shortcuts, storage usage bar,
sort by name/size/date, show/hide hidden files, new folder, copy, cut, paste, rename, delete, multi-select,
open files with whatever app you have installed (Jellyfin, Kodi, a video player...). No internet permission,
no ads, no trackers. The release APK is only a few MB.

## Get the APK (no Android Studio needed)

1. Create a new GitHub repository and upload everything in this folder (keep the `.github` folder).
2. Open the **Actions** tab, pick **Build APK**, press **Run workflow**.
3. When it finishes, open the run and download the **Abyss-apks** artifact (a zip with both APKs).
4. If the build fails, open the failed step and send me the red error text. I'll fix it.

## Install on the Switch (Android)

- Copy the APK to the Switch's Android storage and open it with an installer, or use adb:
  `adb install app-release.apk`
- Open **Abyss Files** from the Android TV home screen and press **Allow access**.

## Setting up the rest of the system (Switchroot)

1. Follow the Switchroot guide for **LineageOS Android TV** (wiki.switchroot.org). Install it through Hekate.
   Do NOT flash the Google apps (GApps) package. That keeps the system small and free of Google ads/bloat.
2. Install apps from their official sources or F-Droid instead of the Play Store:
   Jellyfin (Android TV), Kodi, Plex, and the F-Droid app itself.
3. Optional: install **FLauncher** (open source, no ads) and set it as the home screen.
4. The OS itself is small (a few GB). Most of the partition you choose in Hekate is just space for apps and data.
