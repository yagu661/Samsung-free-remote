# Samsung AI TV Assistant — Direct TV App Installation Guide (No Browser Needed)

**TV Model:** Samsung UA32T4410AKXXL (T4410 Smart HD TV, Tizen 5.5 / 5.0)  
**Phone:** Samsung Galaxy M30s (Android 11) with Termux  
**Package:** `SamsungAssistant.wgt` (Installed Standalone Tizen TV Application)  
**Developer:** Soumya ❤️  

---

## 1. What This Is
You are installing a **real, standalone Samsung TV application** (`SamsungAssistant.wgt`) directly onto your TV's storage.
- It will appear in your **Samsung TV Apps library** with its own icon and title: **"Samsung AI TV Assistant"**.
- You launch it directly with your TV remote just like YouTube or Netflix.
- **You do NOT need to open any web browser on your TV.**

---

## 2. What Is Inside This TV App?
1. **Full YouTube TV Experience:**
   - Dedicated YouTube TV player (`yt-player-box`) with full playback, pause, and volume.
   - Quick Category tabs: **Bengali Songs**, **Arijit Singh Hits**, **Lofi Chill**, **Movies & Trailers**, and **News**.
   - Custom Video Search: Type or speak any query to find and play videos directly on your TV.
2. **Spotify Music Hub:**
   - Built-in Spotify player with curated playlists: **Bengali Top 50**, **Today's Top Hits**, **Chill Lofi**, and **Bollywood Romance**.
3. **Streaming App Launcher:**
   - One-click launch shortcuts for official TV apps (Netflix, Amazon Prime Video, YouTube TV, and Samsung Internet).
4. **AI Assistant Screen & Floating HUD:**
   - Voice assistant HUD overlay that slides up from the bottom (like Google Assistant on Android TV) whenever you speak into your Galaxy M30s remote.
   - Live waveform visualizer and direct TV control.
5. **Full Remote Control & TV D-Pad Integration:**
   - Fully navigable with the Samsung TV remote's D-Pad (Up, Down, Left, Right, Enter/OK, Return/Back, Volume Up/Down, Mute).

---

## 3. Step-by-Step Installation on your Samsung TV (Direct App Install)

### Step 1: Enable Developer Mode on your Samsung TV
1. Turn on your **Samsung UA32T4410** TV.
2. Press the **Home** button on your remote and go to **Apps**.
3. On your Samsung TV remote (or keypad in this Android app), press:
   ```
   1 -> 2 -> 3 -> 4 -> 5
   ```
4. A **Developer Mode Configuration** popup will appear on your TV screen.
5. Switch **Developer Mode** to **ON**.
6. In the **Host PC IP** field, enter your **Galaxy M30s Wi-Fi IP address** (e.g. `192.168.1.15`).
   *(You can find your phone's IP in Android Settings > Wi-Fi > Current Network, or in this app).*
7. Click **OK**.
8. **Cold Reboot the TV:** Press and hold the **Power** button on your TV remote for 5 seconds until the TV turns off and on again.

---

### Step 2: Install and Run in Termux on your Galaxy M30s
1. In Termux, navigate to the `tizen-assistant-app` folder:
   ```bash
   cd tizen-assistant-app
   ```
2. Run the direct installer script:
   ```bash
   python termux_sdb_deploy.py <YOUR_TV_IP>
   ```
   *(Example: `python termux_sdb_deploy.py 192.168.1.105`)*

---

### Step 3: Trigger App Sync on the TV (Downloads & Installs the App)
1. On your Samsung TV, open the **Apps** panel.
2. Open **Options** (top right gear icon or hold the Select/Enter button on any app in the list).
3. Select **Developer Settings** > **App Sync** (or **User App Sync** / **Deep Link Test**).
4. Enter your phone's IP address.
5. The TV will automatically download `SamsungAssistant.wgt` and register **"Samsung AI TV Assistant"** as an installed app.
6. The app icon will appear under **Downloaded Apps**. Select it and enjoy!

---

## 4. How to Download the TV App (.wgt)

You have **3 simple ways** to get the TV app package:

### Method 1: Directly from GitHub Releases (Recommended)
Whenever you push to GitHub, the workflow automatically builds and publishes both files side-by-side:
- **Go to:** Your GitHub Repository > **Releases** > **v-latest**
- Under **Assets**, you will see:
  1. `Samsung-AI-Remote.apk` (Download to your Samsung Galaxy M30s)
  2. `SamsungAssistant.wgt` (Download to your phone or TV)
  3. `Samsung-TV-App-Termux-Installer.zip` (Complete bundle with installer scripts)

### Method 2: From the Android Remote App (No PC or Termux Needed!)
1. Open the **Samsung AI Remote** app on your phone.
2. Go to **Settings** > **Samsung Smart TV App (.wgt) Manager**.
3. Tap **"Start TV Installer Server"**.
4. It will display your phone's IP and serve `widgetlist.xml` and `SamsungAssistant.wgt` directly over your local Wi-Fi.
5. On your TV in Developer Mode, tap **"App Sync"**, and the TV will download and install the app directly from your phone!
6. You can also tap **"Save SamsungAssistant.wgt to Phone Downloads"** to store the file in your phone's Downloads directory.

### Method 3: In Termux via Git
In Termux on your phone, run:
```bash
git pull origin main
cd tizen-assistant-app
python termux_sdb_deploy.py <TV_IP>
```

