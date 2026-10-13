# Samsung AI Remote 📺📱

A complete, mobile-first Samsung Smart TV remote control application for Android phones, tailored for the **Samsung Galaxy M30s (Android 11)** and **Samsung UA32T4410AKXXL Smart TV (Series 4, Tizen OS)**.

---

## ⚡ Direct APK Download

Every time code is pushed to the `main` branch, **GitHub Actions** automatically builds and releases a fresh Android APK:

👉 **[Download Latest APK (Samsung-AI-Remote.apk)](../../releases/tag/v-latest)**

---

## 🚀 Phone-Only Workflow (No PC Required)

Since you are using your phone to manage and run this project, follow this simple workflow:

### 1. Push or Synchronize Code to GitHub
In Google AI Studio, select **Project Settings / GitHub** to push your latest changes to your repository's `main` branch.

### 2. Automated Build Starts
- GitHub Actions automatically starts the workflow **`Build & Release Android APK`**.
- It runs on Ubuntu with Java 21 and Android SDK tools.
- It builds `./gradlew :app:assembleDebug` and packages the APK.

### 3. Check the Build on Your Phone
- On GitHub in your phone's browser, tap the **Actions** tab.
- You will see the latest run with a yellow in-progress dot turning into a green checkmark (**✅ Build Samsung AI Remote APK**).

### 4. Download & Install on Samsung Galaxy M30s
1. Go to the **Releases** section (or tap **`v-latest`**).
2. Under **Assets**, tap **`Samsung-AI-Remote.apk`** to download.
3. Open your phone's **My Files** app > **Downloads**.
4. Tap **Samsung-AI-Remote.apk**.
5. If prompted: *"For your security, your phone is not allowed to install unknown apps from this source"*, tap **Settings** > toggle on **Allow from this source**.
6. Tap **Install** or **Update**.

---

## 🌟 Key Features

1. **Automatic TV Discovery ("Find My Samsung TV")**:
   - Zero manual IP entry required.
   - Discovers Samsung TVs over local Wi-Fi using SSDP/UPnP multicast and subnet probing.
   - Shows TV name, model number, and a 1-tap **Connect & Pair** button.
2. **Complete Remote Controls**:
   - Power On/Standby (with Wake-on-LAN support).
   - Directional circular D-Pad (Up, Down, Left, Right, OK).
   - Volume Rocker (+, -, Mute) and Channel Rocker (+, -).
   - Numeric keypad dialog for direct channel entry (`0-9`).
   - Media controls: Play, Pause, Stop, Rewind, Fast-Forward.
   - System buttons: Home, Back, Menu, Exit, Settings, Source.
   - Direct shortcuts for **YouTube**, **Netflix**, **Prime Video**, and **Spotify**.
3. **Voice Remote**:
   - High-sensitivity microphone button using Android native speech recognition with live text fallback.
   - Deterministic recognition for standard TV phrases (*"Volume up"*, *"Mute the TV"*, *"Go home"*, *"Open YouTube"*, *"Change channel"*).
4. **YouTube Voice Search**:
   - 4-step verified workflow (*"Search YouTube for Bengali songs"*, *"Search YouTube for funny cat videos"*).
   - Focus Search shortcut button (`Left → Up → OK`) and on-screen keyboard navigation.
5. **Gemini AI Command Assistant**:
   - Conversational assistant powered by Gemini.
   - Translates natural-language requests into strict allowlisted TV actions without arbitrary code execution.
6. **Built-in Diagnostics & Settings**:
   - Test suite for network reachability, REST info, WebSocket handshake, and pairing tokens.
   - Configurable voice auto-execution and local credential management.

---

## 🛠️ GitHub Actions Workflow Configuration

- **Workflow File**: `.github/workflows/android-apk.yml`
- **Runner**: `ubuntu-latest`
- **JDK**: Eclipse Temurin OpenJDK 21
- **Gradle Task**: `./gradlew :app:assembleDebug`
- **Output Artifact**: `Samsung-AI-Remote.apk`
- **Release Tag**: `v-latest` (rolling release updated on each push)
- **Permissions**: `contents: write` via `GITHUB_TOKEN`
