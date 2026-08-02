#!/usr/bin/env python3
"""
Tiny local server for StompFlow — run this from the same folder as
index.html, manifest.json, and sw.js.

Usage:
    python3 server.py
Then open: http://localhost:8000

localhost counts as a "secure context" in browsers, so microphone
access (getUserMedia) will work here even though it's blocked on file://.
"""
import http.server
import socketserver

PORT = 8000

Handler = http.server.SimpleHTTPRequestHandler

with socketserver.TCPServer(("", PORT), Handler) as httpd:
    print(f"Serving StompFlow at http://localhost:{PORT}")
    print("Press Ctrl+C to stop.")
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        print("\nStopped.")