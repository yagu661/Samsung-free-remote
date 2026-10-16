#!/usr/bin/env python3
"""
Samsung AI TV Assistant - Termux Direct TV App Installer & SDB Deployer
Target: Samsung UA32T4410AKXXL (T4410 Smart HD TV, Tizen 5.5 / 5.0)
Host: Samsung Galaxy M30s (Android 11 via Termux)
Author: Soumya
"""

import sys
import os
import socket
import http.server
import socketserver
import threading
import time

SDB_PORT = 26101
HTTP_DEPLOY_PORT = 8080

class TizenAppSyncHandler(http.server.SimpleHTTPRequestHandler):
    """Custom HTTP handler that serves widgetlist.xml and SamsungAssistant.wgt to Samsung TV."""
    
    def do_GET(self):
        # Samsung Developer Mode App Sync requests widgetlist.xml
        if self.path == '/widgetlist.xml' or self.path == '/widgetlist.xml/':
            wgt_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "SamsungAssistant.wgt")
            file_size = os.path.getsize(wgt_path) if os.path.exists(wgt_path) else 26296
            local_ip = get_local_ip()
            
            xml_content = f"""<?xml version="1.0" encoding="UTF-8"?>
<rsp stat="ok">
    <list>
        <widget id="aistudio01.SamsungAssistant">
            <title>Samsung AI TV Assistant</title>
            <compression size="{file_size}" type="zip"/>
            <description>AI-Powered Voice Assistant and Entertainment Hub with YouTube and Spotify for Samsung Tizen Smart TV</description>
            <download>http://{local_ip}:{HTTP_DEPLOY_PORT}/SamsungAssistant.wgt</download>
        </widget>
    </list>
</rsp>"""
            self.send_response(200)
            self.send_header('Content-Type', 'application/xml; charset=utf-8')
            self.send_header('Content-Length', str(len(xml_content.encode('utf-8'))))
            self.send_header('Access-Control-Allow-Origin', '*')
            self.end_headers()
            self.wfile.write(xml_content.encode('utf-8'))
            print(f"[✓] Served widgetlist.xml to TV! TV is now downloading SamsungAssistant.wgt...")
            return

        elif self.path == '/SamsungAssistant.wgt':
            print(f"[✓] Samsung TV connected! Transferring SamsungAssistant.wgt to TV storage...")
            super().do_GET()
            print(f"[✓] SamsungAssistant.wgt delivered! Samsung TV will now unpack and register the app.")
            return

        super().do_GET()

def check_tv_connectivity(tv_ip):
    print(f"[*] Checking connection to Samsung Smart TV at {tv_ip}...")
    
    # Check port 8002 (Samsung Smart TV WebSocket)
    sock_8002 = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sock_8002.settimeout(3.0)
    res_8002 = sock_8002.connect_ex((tv_ip, 8002))
    sock_8002.close()
    if res_8002 == 0:
        print(f"[✓] Port 8002 (Samsung Remote Control WebSocket) is OPEN.")
    else:
        print(f"[!] Port 8002 could not be reached. Ensure TV is ON and on the same Wi-Fi.")

    # Check port 26101 (Samsung Developer Mode SDB Port)
    sock_sdb = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sock_sdb.settimeout(3.0)
    res_sdb = sock_sdb.connect_ex((tv_ip, SDB_PORT))
    sock_sdb.close()
    if res_sdb == 0:
        print(f"[✓] Port {SDB_PORT} (Samsung Tizen Developer Mode / SDB) is OPEN!")
        return True
    else:
        print(f"[!] Port {SDB_PORT} is not responding.")
        print(f"    Please verify Developer Mode on your Samsung UA32T4410 TV:")
        print(f"    1. Open 'Apps' panel on TV.")
        print(f"    2. On remote, press 1, 2, 3, 4, 5 in sequence.")
        print(f"    3. Toggle Developer Mode to ON.")
        print(f"    4. Enter your Galaxy M30s IP address as Host PC IP.")
        print(f"    5. Long-press Power button on TV remote for 5 seconds to cold-reboot.")
        return False

def get_local_ip():
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        s.connect(('8.8.8.8', 80))
        ip = s.getsockname()[0]
    except Exception:
        ip = '127.0.0.1'
    finally:
        s.close()
    return ip

def serve_deployment_server(directory):
    os.chdir(directory)
    handler = TizenAppSyncHandler
    with socketserver.TCPServer(("", HTTP_DEPLOY_PORT), handler) as httpd:
        print(f"[✓] Direct App Sync Server listening on port {HTTP_DEPLOY_PORT}...")
        httpd.serve_forever()

def main():
    print("=" * 70)
    print("  SAMSUNG AI TV ASSISTANT - DIRECT TV APP INSTALLER")
    print("  Package: SamsungAssistant.wgt (Standalone Installed TV App)")
    print("  Target:  Samsung UA32T4410AKXXL (T4410 Smart HD TV, Tizen 5.5)")
    print("  Host:    Samsung Galaxy M30s (Android 11 via Termux)")
    print("  Author:  Soumya")
    print("=" * 70)

    if len(sys.argv) < 2:
        print("\nUsage: python termux_sdb_deploy.py <TV_IP_ADDRESS>")
        print("Example: python termux_sdb_deploy.py 192.168.1.105\n")
        sys.exit(1)

    tv_ip = sys.argv[1].strip()
    local_ip = get_local_ip()
    print(f"[*] Phone Local IP (Galaxy M30s): {local_ip}")
    print(f"[*] Target TV IP (UA32T4410):     {tv_ip}\n")

    current_dir = os.path.dirname(os.path.abspath(__file__))
    wgt_file = os.path.join(current_dir, "SamsungAssistant.wgt")

    if not os.path.exists(wgt_file):
        print(f"[*] SamsungAssistant.wgt not found. Packaging now...")
        os.system(f"bash {os.path.join(current_dir, 'termux_package.sh')}")

    sdb_active = check_tv_connectivity(tv_ip)

    print("\n" + "=" * 70)
    print("HOW TO INSTALL DIRECTLY ON YOUR SAMSUNG TV (NO BROWSER):")
    print("=" * 70)
    print("Step 1: On your TV, make sure Developer Mode is ON with Host IP:", local_ip)
    print("Step 2: On your Samsung TV Apps screen:")
    print("        - Press Options / Developer Settings (or hold Select on any app)")
    print("        - Choose 'App Sync' (or 'User App Sync' / 'Deep Link Test')")
    print("        - Enter Server IP:", local_ip)
    print(f"Step 3: The TV immediately downloads 'SamsungAssistant.wgt'")
    print(f"        and installs 'Samsung AI TV Assistant' into your TV Apps list!")
    print(f"Step 4: You will see the new app icon on your TV. Open it directly!")
    print("=" * 70)

    print(f"\n[*] Serving SamsungAssistant.wgt and widgetlist.xml on port {HTTP_DEPLOY_PORT}...")
    print(f"[*] Waiting for Samsung TV to fetch and install the app...")
    print(f"[*] Press Ctrl+C in Termux when installation is complete.\n")
    serve_deployment_server(current_dir)

if __name__ == '__main__':
    main()
