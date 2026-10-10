# AYN Thor WoW Launcher

> **A fork of [WoW Forever for Android](https://github.com/jaredgei/wow-forever-android) by [jaredgei](https://github.com/jaredgei).** The WoW launcher, the single-container setup, native ARM64 launch and the bundled runtime all come from jaredgei's project, which is itself a fork of GameNative. This fork adds Retail, Classic Era, TBC Anniversary and MoP Classic support, a styled launcher, a second-screen button pad with settings, controller cursor mode and a themed in-game menu for the AYN Thor. Please star and support the original.


Play **World of Warcraft** on Snapdragon Android handhelds as a normal Android app, with extra work for the dual-screen **AYN Thor**. Pick **Forever**, **Retail** or **Classic** (Classic Era, TBC Anniversary or MoP Classic), press Play, and the game runs.

This uses Blizzard's own **Windows ARM64** WoW clients, so the game itself runs natively on the device's CPU. Wine translates the Windows calls, and DXVK plus a patched Turnip Vulkan driver render the game on the Adreno GPU. Nothing is emulated as x86.

### Highlights of this fork

- **Five clients in one app:** Forever (beta), Retail, Classic Era, TBC Anniversary and MoP Classic, each with its own game folder, in-app updates and Play button. Tap **Classic** and a second row opens with the three Classic clients. The ARM64 clients for TBC Anniversary and MoP Classic download from Blizzard's CDN and run fine.
- **Per-game switches on the main screen:** the Thor button pad, forcing WoW's gamepad UI, and R3 mouse/camera toggling can each be set separately for every client.
- **Thor second-screen button pad:** a touch pad on the bottom screen with a trackpad, keyboard, hotbar-style buttons, window shortcuts (Map, Character, Spellbook, Bags, Group Finder and more), modifier and F-keys, a scroll area, a Gboard-style keyboard, and a full settings page. Every button can be remapped and renamed. See [The Thor button pad](#the-thor-button-pad).
- **Controller mouse mode:** the right stick becomes a cursor on R3, with A/B as clicks, Y/X as scroll up/down and an adjustable speed ramp. R3 toggling can be switched off per game, and then R3 stays a normal button press.
- **App updates you control:** the app updates from this repo's releases, with an opt-in for beta builds and an option to skip automatic app updates.
- **World of Warcraft look:** a stone-and-gold launcher and in-game quick menu, with the chosen client named on the Play button and boot screen.
- **Its own app:** package `app.aynthorwow`, so it installs next to jaredgei's original WoW Forever app, GameNative and Winlator.

> Unofficial community project. Not affiliated with or endorsed by Blizzard Entertainment. You need your own Battle.net account with access to the client you want to play (the Forever beta needs beta access). This repo and its releases contain **no** Blizzard game files.

---

## Supported devices

| Device | Status |
| :--- | :--- |
| AYN Thor (Snapdragon 8 Gen 2 / Adreno 740) | Main target of this fork: tested with controller support and the second-screen pad |
| Retroid Pocket 6 (Snapdragon 8 Gen 2 / Adreno 740) | Tested by the original community setup |
| AYN Odin 2 / Mini (Snapdragon 8 Gen 2) | Expected to work |
| AYN Odin Portal (Snapdragon 8 Elite / Adreno 830) | Supported via bundled Turnip v32 driver |
| RedMagic 11 Pro (Snapdragon 8 Elite / Adreno 840) | Tested in-world by arusiasotto |
| Samsung Galaxy S23/S24/S25 / Z Fold series | Supported (with automatic Samsung UBWC optimization) |

Requirements:

- Snapdragon 8 series (Adreno 7xx or Adreno 8xx). The app bundles both the Adreno 740 driver and the Turnip v32 driver for Snapdragon 8 Elite / Adreno 8xx, automatically detecting the GPU and applying the optimal driver and environment flags.
- Android 10 or newer, 64-bit.
- About **80 GB** free for the game data (internal storage or SD card), plus about 4 GB of internal storage for the app and its Windows environment.
- A Mac or PC with the client you want installed through Battle.net, to copy the game data from (one-time).

---

## Installation

You need a PC or Mac with World of Warcraft installed through Battle.net, and an Android device from the [supported list](#supported-devices) with about 80 GB free (internal storage or an SD card).

### 1. Install the app

Download **WoWLauncher.apk** from the [releases page](https://github.com/wyattabuntjer/AYN-Thor-WoW-Launcher/releases/latest) onto your Android device and install it. If Android asks, allow installs from your browser or file manager.

### 2. Get the game installed on your PC or Mac

Install every version of World of Warcraft you want to play on Android (Retail, Classic Era, TBC Anniversary, MoP Classic and so on) through the Battle.net app.

> **Important:** you can't choose which versions go into the files you copy in the next step. Whatever is installed gets copied. If a version is installed that you **don't** want on your Android device, uninstall it before you copy anything.

### 3. Copy the game data to your Android device

From your computer's World of Warcraft folder, copy these two items onto the Android device:

- the **`Data`** folder
- the **`.build.info`** file

The World of Warcraft folder is normally here:

| Computer | Default location |
| :--- | :--- |
| Windows | `C:\Program Files (x86)\World of Warcraft\` |
| Mac | `/Applications/World of Warcraft/` |

Put them anywhere on the device, just remember where. I recommend creating a **WoW** folder in the device's main storage or on an SD card, and putting both inside it.

- `.build.info` is a hidden file. Turn on hidden files (Windows: View → Show → Hidden items; Mac: press **Cmd + Shift + .** in Finder) so you can see and copy it. Without it, the game won't start.
- You can copy over USB (set the device to File Transfer mode) or with a microSD card.

### 4. Open the app and point it at your files

1. Open the app. When it asks for the game files, tap **Locate Game Files** and choose the folder that contains `Data` and `.build.info`.
2. Give the app the storage permission it asks for.
3. Set up your Battle.net auto-login, or turn auto-login off.

### 5. Press Play

Pick your game at the top of the screen (**Classic** opens Classic Era, TBC Anniversary and MoP Classic) and press **PLAY**. The first launch of each game downloads its ARM64 client and sets up the Windows environment. That takes a few minutes and needs an internet connection. After that, game updates happen inside the app.

- **Setup screen:** to open folder settings, forget credentials or check status, hold **Start + Select + L2 + R2** (or tap the back button) during the loading splash to cancel boot and return to the setup screen.

### Controls and signing in

- **Cursor mode (R3):** click the right stick to turn it into a mouse cursor. **A** and **B** click (left/right, or swapped, or off in the pad settings). **Y** and **X** scroll up and down, with a scroll speed setting and an invert option. Speed, ramp and deadzone are adjustable. Click R3 again to go back to normal camera control.
  - The **R3 toggles mouse / camera** switch on the main screen turns this off per game. With it off, R3 is a regular button press, and the R3 section of the pad settings is hidden. Profiles can't change this switch during play.
- **Controller:** built-in handheld controllers work in-game. WoW's own gamepad mode handles the mapping.
- **In-game menu:** press Back (the button or the back swipe gesture) to open the sidebar. It has **Keyboard**, on-screen controls, performance overlay and **Exit**.
- **Keyboard:** the sidebar's **Keyboard** opens the Android keyboard. On dual-screen devices like the Thor it appears on the bottom screen. Symbols like `@` work, and so does pasting. The button pad also has its own on-screen keyboard laid out like Gboard (number row, QWERTY, a `?123` symbols page, `/` on the main page for chat commands).
- **Battle.net Auto-Login:** configure credentials directly on the launcher setup screen (**Configure Login** / **Update Login**). Credentials are saved encrypted on-device via Android Keystore. On boot, the launcher generates `_classic_beta_/login.txt`, which the WoW client automatically reads on startup to sign in natively without macro simulation or synthetic clicks. Use **Forget Saved Login** on the launcher setup screen to clear credentials and remove the login file.
  - Authenticator codes still have to be entered by hand.

---

## The Thor button pad

On the AYN Thor the bottom screen shows a touch pad while you play. The header has three buttons: **trackpad**, **settings (gear)** and **keyboard**. The pad can be switched on or off per client with the **Action button pad** switch on the main screen.

- **Trackpad:** a touch mouse with Shift, Ctrl and Alt always available, left and right click buttons, and a **scroll area** along one edge: slide a finger up or down to scroll. Its side, speed and direction are adjustable and it can be turned off.
- **Buttons:** hotbar numbers, F-keys, modifiers and window shortcuts (Map, Character, Spellbook, Talents, Skills, Quest Log, Social, System, Bags, Group Finder, Achievements, Guild). Every window button shows on every client, including ones a client doesn't have, because any button can be renamed and remapped. When more than eight window buttons are shown, they split into two columns.
- **Keyboard:** a Gboard-style on-screen keyboard with a number row, shift and caps, and a `?123` symbols page with arrow keys.
- **Settings page** (the gear) lets you change:
  - which modifier keys, window buttons and pad sections are shown (the rest resize to fill the space)
  - how many F-keys show (any number from F1-F3 up to F1-F12), swap left/right sides, label size, haptics and double-tap lock time
  - trackpad speed, acceleration, tap-to-click and the scroll area
  - right-stick cursor speed, ramp, deadzone, A/B click mode and Y/X scroll (shown only when R3 toggling is on)
  - **remap and rename:** tap any window, number or F-key button to choose the key it sends, or give it a new name (up to 12 characters) typed on the on-screen keyboard. Shift, Ctrl, Alt and the trackpad click buttons are fixed. Use **Reset all remaps** or **Reset all names** to undo.
  - three saved profiles (with Save / Overwrite), backup to the clipboard, and reset
  - which client launches next, and a helper for `Config.wtf`

---

## Updating the game

The launcher has a native in-app game updater connected directly to Blizzard's public edge CDNs, for each client. **You no longer need a computer or USB cables to keep your game updated.**

Whenever Blizzard patches the game:
1. **Live Version Check:** The launcher automatically checks the live version against Blizzard's public patch service on startup.
2. **Update Gate:** If an update is detected, auto-launch cancels and displays the update card with the remote build version, plus a secondary *Launch Anyway (Outdated)* override.
3. **One-Tap Update:** Tapping **UPDATE TO {version}** downloads the new ARM64 client binaries (`WowB-ARM64.exe` and companion DLLs), saves remote configs to `Data/config/`, syncs new CASC index archives to `Data/indices/`, and atomically updates `.build.info` on disk over Wi-Fi.
4. **Instant Boot:** Once the update completes, the launcher automatically transitions into booting World of Warcraft at 60 FPS.
5. **In-Game Streaming:** The game engine's internal streaming client seamlessly streams any newly introduced assets from Blizzard's edge CDNs during gameplay.

*(Optional fallback: If you ever want to re-seed or mirror your full PC installation over USB, `tools/sync_wow_to_device.sh` is still available.)*

The app itself updates from this repo's releases: when a newer release with an APK is published, the launcher offers it. An app update never launches the game by itself. Two options sit at the bottom of the launcher screen: **Beta app updates** (also offers pre-releases such as `2.4.1b`) and **Skip automatic app updates** (stops the automatic check; **Refresh Status** still checks on demand).

---

## Troubleshooting

| Symptom | Fix |
| :--- | :--- |
| *CAS system was unable to initialize: no active install info entries* | `.build.info` or `_classic_beta_/.flavor.info` is missing on the device. |
| *No realms available* / no servers listed | The device client is out of date. Tap **Update** on the launcher setup screen, or restart the app with Wi-Fi enabled. |
| Retail, Classic Era, TBC Anniversary or MoP Classic won't start | Support for these is newer and less tested than Forever. Check the client's `Errors/` folder (`_retail_`, `_classic_era_`, `_anniversary_` or `_classic_`) and open an issue with what you find. |
| Keyboard doesn't appear | Force-stop Gboard (Settings → Apps → Gboard → Force stop) and open **Keyboard** again. It can get stuck on the second screen. |
| Returns to the launcher after "Launching Game…" | Check the files under `_classic_beta_/Errors/` on the device. |
| Handheld frontend (e.g. Cocoon) shows the wrong icon | The frontend cached an old icon. Set it with the frontend's "Edit App Artwork", or reinstall the app. |

---

## Building from source

Requirements: JDK 17 and the Android SDK (with build-tools and platform-tools).

```bash
tools/fetch_components.sh           # Wine/Proton, DXVK and Turnip archives (~130 MB, not in git)
./gradlew assembleModernRelease     # app/build/outputs/apk/modern/release/app-modern-release.apk
./gradlew assembleModernDebug       # unminified debug build, faster to iterate on
```

Release builds are signed with `app/keystores/keystore.properties` when it exists (`storeFile`, `storePassword`, `keyAlias`, `keyPassword`). Otherwise they fall back to the debug key. Builds signed with different keys can't update each other, so switching between them means uninstalling first.

`fetch_components.sh` downloads the three runtime archives from jaredgei's `components-v1` release and checks them against `tools/components.sha256`. If you already have them, pass a folder instead: `tools/fetch_components.sh /path/to/components`.

---

## Credits and how it works

This project packages other people's work into a single-purpose app. None of it would exist without:

- **[jaredgei/wow-forever-android](https://github.com/jaredgei/wow-forever-android)** by **jaredgei**. This repo is a fork of it. The WoW launcher screen, the pre-configured container, native ARM64 launch, the Battle.net client downloader, the bundled runtime components and most of the Thor fixes described below were done there. Thank you.

- **[GameNative](https://github.com/utkarshdalal/GameNative)** by Utkarsh Dalal and contributors (GPL-3.0). The Android app, the Wine container management and the X server are all GameNative, based on v1.2.1. GameNative in turn builds on **[Pluvia](https://github.com/oxters168/Pluvia)**, **[Winlator](https://github.com/brunodev85/winlator)**, **[Winlator Cmod](https://github.com/coffincolors/winlator)** and the **[Bionic Vulkan wrapper](https://github.com/leegao/bionic-vulkan-wrapper)**.
- **The WoW Forever RP6 community bundle**, which first got the beta running on a Retroid Pocket 6 in GameNative and supplied the three custom runtime components:
  - **Proton 11 ARM64EC** built from [The412Banner/proton-wine](https://github.com/The412Banner/proton-wine/tree/e5fa703ed7f7329e20d7ede481ab185cf8b1a8b2) with an ARM64 copied-syscall fix and an NLS fallback allocation patch.
  - **DXVK 2.4.1 (aarch64)** built from [doitsujin/dxvk](https://github.com/doitsujin/dxvk/tree/0cf05780abd7250c2cd713b7749cf32180157cf5).
    - **Mesa Turnip (Adreno 740)** built from Mesa [`fe067b17d9`](https://github.com/mirror/mesa/tree/fe067b17d9) with a patch limiting barycentric waits to the current block, which fixes a shader scheduler assertion on Adreno 740.
  - **Mesa Turnip (Adreno 8xx / Snapdragon 8 Elite)** built by **[arusiasotto](https://github.com/arusiasotto/wow-forever-a840)** from **[whitebelyash/mesa-unified](https://github.com/whitebelyash/mesa-unified)** (`turnip/gen8` v32) with the barycentric scheduler fix and ICD exports.
- **[Wine](https://www.winehq.org/)**, **[Proton](https://github.com/ValveSoftware/Proton)**, **[DXVK](https://github.com/doitsujin/dxvk)**, **[Mesa](https://mesa3d.org/)**, **[FEX-Emu](https://github.com/FEX-Emu/FEX)**, **[box64](https://github.com/ptitSeb/box64)** and **[PulseAudio](https://www.freedesktop.org/wiki/Software/PulseAudio/)**.
- **Blizzard Entertainment** for World of Warcraft and its Windows ARM64 client. The app icon and splash art come from Blizzard's official [WoW Forever page](https://worldofwarcraft.blizzard.com/en-us/forever). Blizzard owns them and the World of Warcraft marks.

Full third-party license details are in [`THIRD_PARTY_NOTICES`](THIRD_PARTY_NOTICES).

### What this fork adds on top of WoW Forever for Android

- Forever, Retail, Classic Era, TBC Anniversary and MoP Classic selectable from the launcher, each with its own game folder and in-app updates.
- Per-client switches for the button pad, forcing the gamepad UI and R3 mouse/camera toggling.
- A stone-and-gold launcher theme and quick menu, and the selected client named on the Play button and boot screen.
- Second-screen button pad with a settings page: grouped buttons, window shortcuts, modifier and F-key options, remapping and renaming of every button, profiles, a trackpad scroll area, a Gboard-style keyboard, and trackpad and stick tuning.
- Right-stick cursor mode on R3 with A/B clicks, Y/X scrolling and a speed ramp.
- An updater with beta opt-in and a skip-automatic-updates option.
- Its own package name (`app.aynthorwow`) and an updater that follows this repo's releases.

### What WoW Forever for Android changes from GameNative

GameNative is a general game library with Steam, GOG, Epic, Amazon, EA and Rockstar stores, mod management, VR support and per-game container settings. This fork turns it into a launcher for one pre-configured container:

- **Standalone identity:** package `app.aynthorwow`, WoW name, icons, banners and splash screen, installable next to GameNative.
- **Direct launch & instant boot:** the app opens to a WoW splash screen (`ui/screen/wow/WoWForeverScreen.kt`) and automatically boots straight into the game once configured. Holding **Start + Select + L2 + R2** or pressing Back on the loading screen cancels boot to return to the setup screen.
- **Pre-configured container:** bionic, Proton 11 ARM64EC, Turnip through the Vulkan wrapper, DXVK 2.4.1 aarch64, WINEESYNC off, all 8 cores, 1920x1080, and `G:` mapped to `/storage/emulated/0/WoW Forever`.
- **Native ARM64 launch:** GameNative wraps every Windows program in `winhandler.exe`, an x86-64 helper that needs x86 emulation. ARM64 executables now launch directly, so the game never goes through FEX, and the working directory is set from the mapped drive.
- **Game file setup:** the launcher writes `_classic_beta_/.flavor.info` and a basic `WTF/Config.wtf` if they're missing, forces `gxApi "D3D11"` (so a copied Mac config that says Metal can't break it), and refuses to start without `.build.info`.
- **Package-name fixes:** several paths were hard-coded to `app.gamenative`: the bionic library path rewrite (`WINEMU_HOST_PKG` / `HOST_PKG`), the gamepad shared-memory files, the DXVK state cache and the default drives. The controller path was the reason controllers didn't work.
- **Small robustness fixes:** non-numeric container IDs no longer crash the ID parser, and the bionic redirect library is copied in if it's missing.
- **Keyboard fixes:** GameNative's on-screen keyboard dropped shifted symbols (`@` came through as `2`), because it sent key codes without the character or the Shift key. The fix is in `Keyboard.java` and `IMEInputReceiver.kt`. The keyboard now always goes through the IME receiver, so it appears on the Thor's bottom screen.
- **Battle.net auto-login:** native `login.txt` client authentication managed via Keystore-encrypted credentials (`ui/screen/wow/BattleNetSignIn.kt`). Eliminates coordinate-based typing macros in favor of the game client's built-in startup authentication, with configuration available directly from the launcher setup screen.
- **Native In-App Game Updater (v2.0):** Live version check against Blizzard patch services, direct-from-CDN ARM64 binary and manifest downloads, CASC index synchronization, and atomic `.build.info` updates on-device over Wi-Fi without any PC dependency.
- **Lean runtime & startup:** eliminated continuous background accelerometer polling during gameplay in favor of native OS window management, removed cold-boot bitmap allocations, guarded background performance metric collection loops, and switched DNS to native platform resolution.
- **Removed:** every store backend (Steam and JavaSteam, GOG, Epic, Amazon, EA, Rockstar), library, login, settings, custom game scanner, and downloads screens, ExoPlayer and browser dependencies, `DownloadService`, `ContainerMigrator`, Nexus mod management, the Meta Quest/XR build, PostHog analytics, Play Integrity, the self-updater, the Room database, the notification prompt, extra bundled Box64/FEX/DXVK versions and the legacy (Android 9) build. That's roughly 135k lines of code and about half the APK size.
- **Tooling:** `tools/download_wow_arm64.py` fetches the ARM64 client from Blizzard's CDN, `tools/sync_wow_to_device.sh` handles updates, and `tools/fetch_components.sh` downloads the runtime archives.

## License

GPL-3.0, the same as GameNative. See [`LICENSE`](LICENSE). The bundled runtime components carry their own licenses (LGPL, MIT and others). See `THIRD_PARTY_NOTICES` and the notices inside each component archive.
