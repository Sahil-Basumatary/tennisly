#!/usr/bin/env python3
"""Header-faithful stub of the gateway public /api/v1 surface for local ZAP triage.

Mirrors SecurityConfig response headers and X-Api-Key auth so rule noise can be
ratcheted without bringing the full Spring stack up. Not a substitute for a
staging run against the real gateway.
"""
from __future__ import annotations

import json
import os
import sys
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

API_KEY = os.environ.get("API_KEY", "")
HOST = os.environ.get("ZAP_STUB_HOST", "0.0.0.0")
PORT = int(os.environ.get("ZAP_STUB_PORT", "18080"))

SECURITY_HEADERS = {
    "X-Content-Type-Options": "nosniff",
    "X-Frame-Options": "DENY",
    "Strict-Transport-Security": "max-age=31536000; includeSubDomains",
    "Referrer-Policy": "no-referrer",
    "Permissions-Policy": "camera=(), microphone=(), geolocation=()",
    "Cache-Control": "no-store",
}


class Handler(BaseHTTPRequestHandler):
    server_version = "tennisly-gateway-stub"
    sys_version = ""

    def log_message(self, fmt: str, *args) -> None:
        sys.stderr.write("%s - %s\n" % (self.address_string(), fmt % args))

    def _write(self, code: int, body: bytes, content_type: str = "application/json") -> None:
        self.send_response(code)
        self.send_header("Content-Type", content_type)
        self.send_header("Content-Length", str(len(body)))
        for key, value in SECURITY_HEADERS.items():
            self.send_header(key, value)
        self.end_headers()
        if self.command != "HEAD":
            self.wfile.write(body)

    def _unauthorized(self) -> None:
        self._write(401, b'{"error":"unauthorized"}')

    def _authorized(self) -> bool:
        key = self.headers.get("X-Api-Key", "")
        return bool(API_KEY) and key == API_KEY

    def do_OPTIONS(self) -> None:
        self.send_response(204)
        for key, value in SECURITY_HEADERS.items():
            self.send_header(key, value)
        self.send_header("Access-Control-Allow-Origin", "http://localhost:3000")
        self.send_header("Access-Control-Allow-Headers", "X-Api-Key, Content-Type")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        self.end_headers()

    def do_HEAD(self) -> None:
        self.do_GET()

    def do_GET(self) -> None:
        path = self.path.split("?", 1)[0]
        if path in ("/actuator/health", "/health"):
            self._write(200, b'{"status":"UP"}')
            return
        if path.startswith("/actuator/"):
            self._write(403, b'{"error":"forbidden"}')
            return
        if not path.startswith("/api/v1/"):
            self._write(404, b'{"error":"not_found"}')
            return
        if not self._authorized():
            self._unauthorized()
            return
        if path.endswith("/webhooks") or path.rstrip("/").endswith("webhooks"):
            self._write(200, b"[]")
            return
        self._write(200, b"[]")

    def do_POST(self) -> None:
        path = self.path.split("?", 1)[0]
        length = int(self.headers.get("Content-Length", "0") or "0")
        if length:
            self.rfile.read(length)
        if not path.startswith("/api/v1/"):
            self._write(404, b'{"error":"not_found"}')
            return
        if not self._authorized():
            self._unauthorized()
            return
        self._write(201, b"{}")


def main() -> int:
    if not API_KEY:
        print("set API_KEY for the stub", file=sys.stderr)
        return 1
    server = ThreadingHTTPServer((HOST, PORT), Handler)
    print(json.dumps({"stub": "listening", "host": HOST, "port": PORT}), flush=True)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
