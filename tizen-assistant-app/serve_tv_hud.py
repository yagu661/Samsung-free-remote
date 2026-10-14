#!/usr/bin/env python3
"""
Samsung AI TV Assistant - Instant Zero-Install TV Browser Server
Run this script in Termux on Galaxy M30s.
Then open http://<phone-ip>:8080 on the Samsung TV Internet Browser.
Author: Soumya
"""

import http.server
import socketserver
import socket
import os

PORT = 8080

def get_ip():
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        s.connect(('8.8.8.8', 80))
        ip = s.getsockname()[0]
    except Exception:
        ip = '127.0.0.1'
    finally:
        s.close()
    return ip

def main():
    current_dir = os.path.dirname(os.path.abspath(__file__))
    os.chdir(current_dir)
    local_ip = get_ip()

    print("=" * 60)
    print("  SAMSUNG AI TV ASSISTANT - INSTANT TV BROWSER HUD")
    print("  Made with love by Soumya ❤️")
    print("=" * 60)
    print(f"\n[✓] Server running from Galaxy M30s phone!")
    print(f"\n--> On your Samsung UA32T4410 TV:")
    print(f"    1. Open the built-in 'Internet' (Samsung Web Browser) app.")
    print(f"    2. Type this exact URL into the TV address bar:")
    print(f"       http://{local_ip}:{PORT}/")
    print(f"    3. Press Enter / Go.")
    print(f"\n[✓] The 10-foot TV Assistant interface will display fullscreen.")
    print(f"[✓] Press Ctrl+C in Termux when done.\n")
    print("=" * 60)

    handler = http.server.SimpleHTTPRequestHandler
    with socketserver.TCPServer(("", PORT), handler) as httpd:
        try:
            httpd.serve_forever()
        except KeyboardInterrupt:
            print("\n[*] Server stopped.")

if __name__ == '__main__':
    main()
