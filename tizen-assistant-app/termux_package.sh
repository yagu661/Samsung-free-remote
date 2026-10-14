#!/bin/bash
# ==============================================================================
# Samsung Tizen TV Assistant - Termux Packaging Script
# Phone: Samsung Galaxy M30s (Android 11) with Termux
# TV: Samsung UA32T4410AKXXL (T4410 Smart HD TV, Tizen 5.5)
# Created with love by Soumya
# ==============================================================================

set -e

echo "--------------------------------------------------------"
echo "  SAMSUNG AI TV ASSISTANT - TERMUX WGT PACKAGER"
echo "--------------------------------------------------------"

# 1. Check required tools in Termux
if ! command -v zip &> /dev/null; then
    echo "[!] 'zip' utility is not installed in Termux."
    echo "[*] Installing zip via pkg..."
    pkg update -y && pkg install -y zip
fi

# 2. Navigate to project directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

OUTPUT_WGT="SamsungAssistant.wgt"

# 3. Clean any existing package
if [ -f "$OUTPUT_WGT" ]; then
    rm -f "$OUTPUT_WGT"
    echo "[*] Removed existing $OUTPUT_WGT"
fi

# 4. Verify critical Tizen manifest files
if [ ! -f "config.xml" ]; then
    echo "[ERROR] config.xml not found! Cannot build Tizen package."
    exit 1
fi

if [ ! -f "index.html" ]; then
    echo "[ERROR] index.html not found! Cannot build Tizen package."
    exit 1
fi

# 5. Build .wgt (Tizen Widget format: standard zip containing config.xml at root)
echo "[*] Packaging Tizen Web Application into $OUTPUT_WGT..."
zip -r "$OUTPUT_WGT" config.xml index.html css/ js/ icon.png -x "*.git*" "*.DS_Store*"

FILESIZE=$(stat -c%s "$OUTPUT_WGT" 2>/dev/null || stat -f%z "$OUTPUT_WGT")
echo "--------------------------------------------------------"
echo " [SUCCESS] Created $OUTPUT_WGT ($FILESIZE bytes)"
echo " Location: $SCRIPT_DIR/$OUTPUT_WGT"
echo "--------------------------------------------------------"
echo ""
echo "Next Steps for Samsung UA32T4410 Developer Mode Installation:"
echo "1. On TV: Open Apps > Press 1, 2, 3, 4, 5 on remote"
echo "2. Turn Developer Mode ON -> Enter Phone's Wi-Fi IP"
echo "3. Restart TV by holding remote Power button for 5 seconds"
echo "4. In Termux, run: python termux_sdb_deploy.py <TV_IP>"
echo "   OR for Instant Zero-Install Mode, run: python serve_tv_hud.py"
echo "--------------------------------------------------------"
