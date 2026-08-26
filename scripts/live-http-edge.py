#!/usr/bin/env python3
"""Local CDN stand-in for near-live HTTP cache-collapse evidence. Not Vercel."""

from __future__ import annotations

import argparse
import json
import threading
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import parse_qs, urlparse

MATCH_ID = "aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee"
START = time.time()
ORIGIN_LOCK = threading.Lock()
ORIGIN_FETCHES = 0
SEQUENCE = 1
POINT_INTERVAL_S = 20.0
EVENTS_TTL_S = 1.0


def live_sequence() -> int:
    interval = max(POINT_INTERVAL_S, 0.001)
    return 1 + int((time.time() - START) / interval)


def etag_for(prefix: str, sequence: int) -> str:
    return f'"{prefix}-{sequence}"'


def live_body(sequence: int) -> bytes:
    payload = {
        "id": MATCH_ID,
        "status": "IN_PROGRESS",
        "liveSequence": sequence,
        "pointsPlayed": sequence,
        "updatedAt": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
        "currentScore": {"points": ["30", "15"]},
    }
    return json.dumps(payload, separators=(",", ":")).encode("utf-8")


def cursor_body(sequence: int) -> bytes:
    payload = {
        "id": MATCH_ID,
        "status": "IN_PROGRESS",
        "liveSequence": sequence,
        "pointsPlayed": sequence,
    }
    return json.dumps(payload, separators=(",", ":")).encode("utf-8")


def ticker_body(sequence: int) -> bytes:
    payload = {
        "updatedAt": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
        "items": [
            {
                "id": MATCH_ID,
                "status": "live",
                "tournament": "US Open",
                "round": "R2",
                "href": f"/matches/{MATCH_ID}",
                "liveSequence": sequence,
            }
        ],
    }
    return json.dumps(payload, separators=(",", ":")).encode("utf-8")


def events_body(after: int, sequence: int) -> bytes:
    rows = [{"id": f"evt-{n}", "sequence": n, "eventType": "MATCH_POINT_RECORDED"} for n in range(after + 1, sequence + 1)]
    return json.dumps(rows, separators=(",", ":")).encode("utf-8")


class OriginHandler(BaseHTTPRequestHandler):
    def log_message(self, format, *args):
        return

    def do_GET(self):
        global ORIGIN_FETCHES, SEQUENCE
        parsed = urlparse(self.path)
        if parsed.path == "/metrics":
            with ORIGIN_LOCK:
                body = json.dumps(
                    {"originFetches": ORIGIN_FETCHES, "liveSequence": SEQUENCE}
                ).encode("utf-8")
            self._send(200, body, "no-store")
            return
        with ORIGIN_LOCK:
            ORIGIN_FETCHES += 1
            SEQUENCE = live_sequence()
            sequence = SEQUENCE
        if parsed.path.endswith("/ticker"):
            body = ticker_body(sequence)
            self._send(200, body, "public, s-maxage=3, stale-while-revalidate=10", etag_for("ticker", sequence))
            return
        if parsed.path.endswith("/cursor"):
            body = cursor_body(sequence)
            self._send(200, body, "public, s-maxage=2, stale-while-revalidate=3", etag_for(f"cursor-{MATCH_ID}", sequence))
            return
        if parsed.path.endswith("/live"):
            body = live_body(sequence)
            self._send(200, body, "public, s-maxage=2, stale-while-revalidate=3", etag_for(f"live-{MATCH_ID}", sequence))
            return
        if "/events" in parsed.path:
            query = parse_qs(parsed.query)
            after = int(query.get("afterSequence", ["0"])[0] or 0)
            body = events_body(after, sequence)
            self._send(200, body, "private, no-store, no-cache, must-revalidate")
            return
        self._send(404, b'{"error":"not found"}', "private, no-store")

    def _send(self, status, body, cache_control, etag=None):
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Cache-Control", cache_control)
        self.send_header("Content-Length", str(len(body)))
        if etag:
            self.send_header("ETag", etag)
        self.end_headers()
        self.wfile.write(body)


class QueuedThreadingHTTPServer(ThreadingHTTPServer):
    daemon_threads = True
    allow_reuse_address = True
    request_queue_size = 1024


class CachedEntry:
    def __init__(self, status, headers, body, expires_at):
        self.status = status
        self.headers = headers
        self.body = body
        self.expires_at = expires_at
        self.stored_at = time.time()


class EdgeHandler(BaseHTTPRequestHandler):
    cache = {}
    inflight = {}
    origin_host = "127.0.0.1"
    origin_port = 18080
    origin_timeout = 5.0
    origin_fetches = 0
    lock = threading.Lock()

    def log_message(self, format, *args):
        return

    def do_GET(self):
        parsed = urlparse(self.path)
        if parsed.path == "/metrics":
            with ORIGIN_LOCK:
                body = json.dumps(
                    {
                        "originFetches": EdgeHandler.origin_fetches,
                        "liveSequence": SEQUENCE,
                    }
                ).encode("utf-8")
            self._client(200, {"Cache-Control": "no-store", "Content-Type": "application/json"}, body, "BYPASS", 0)
            return
        if "/events" in parsed.path:
            self._events(parsed)
            return
        cache_key = "GET:" + parsed.path
        cached = self._cached(cache_key, time.time())
        if cached:
            self._serve_cached(cached, "HIT")
            return
        owner, event = self._claim(cache_key)
        if not owner:
            event.wait(2.0)
            cached = self._cached(cache_key, time.time())
            if cached:
                self._serve_cached(cached, "HIT")
                return
            status, headers, body = self._origin()
            self._client(status, headers, body, "MISS", 0)
            return
        try:
            status, headers, body = self._origin()
            ttl = self._s_maxage(headers.get("Cache-Control", ""))
            if ttl > 0 and status == 200:
                with self.lock:
                    self.cache[cache_key] = CachedEntry(
                        status, headers, body, time.time() + ttl
                    )
        finally:
            self._release(cache_key, event)
        inm = self.headers.get("If-None-Match")
        etag = headers.get("ETag")
        if inm and etag and inm == etag:
            self._client(304, headers, b"", "MISS", 0)
            return
        self._client(status, headers, body, "MISS", 0)

    def _events(self, parsed):
        cache_key = "GET:" + parsed.path + "?" + parsed.query
        cached = self._cached(cache_key, time.time())
        if cached:
            self._serve_cached(cached, "HIT")
            return
        owner, event = self._claim(cache_key)
        if not owner:
            event.wait(2.0)
            cached = self._cached(cache_key, time.time())
            if cached:
                self._serve_cached(cached, "COLLAPSE")
                return
            status, headers, body = self._origin()
            self._client(status, headers, body, "BYPASS", 0)
            return
        try:
            status, headers, body = self._origin()
            if status == 200:
                with self.lock:
                    self.cache[cache_key] = CachedEntry(
                        status, headers, body, time.time() + EVENTS_TTL_S
                    )
        finally:
            self._release(cache_key, event)
        self._client(status, headers, body, "BYPASS", 0)

    def _cached(self, cache_key, now):
        with self.lock:
            entry = self.cache.get(cache_key)
            if entry and entry.expires_at > now:
                return entry
            return None

    def _serve_cached(self, entry, cache):
        age = int(time.time() - entry.stored_at)
        inm = self.headers.get("If-None-Match")
        etag = entry.headers.get("ETag")
        if inm and etag and inm == etag:
            self._client(304, entry.headers, b"", cache, age)
            return
        self._client(entry.status, entry.headers, entry.body, cache, age)

    def _claim(self, cache_key):
        with self.lock:
            existing = self.inflight.get(cache_key)
            if existing is not None:
                return False, existing
            event = threading.Event()
            self.inflight[cache_key] = event
            return True, event

    def _release(self, cache_key, event):
        with self.lock:
            self.inflight.pop(cache_key, None)
        event.set()

    def _origin(self):
        import http.client

        with ORIGIN_LOCK:
            EdgeHandler.origin_fetches += 1
        conn = http.client.HTTPConnection(
            self.origin_host, self.origin_port, timeout=self.origin_timeout
        )
        try:
            conn.request("GET", self.path, headers={"Accept": "application/json"})
            response = conn.getresponse()
            body = response.read()
            headers = {key: value for key, value in response.getheaders()}
            return response.status, headers, body
        finally:
            conn.close()

    def _s_maxage(self, control):
        for part in control.split(","):
            item = part.strip()
            if item.startswith("s-maxage="):
                try:
                    return max(0, int(item.split("=", 1)[1]))
                except ValueError:
                    return 0
        return 0

    def _client(self, status, headers, body, cache, age):
        self.send_response(status)
        for key, value in headers.items():
            if key.lower() in {"content-length", "transfer-encoding"}:
                continue
            self.send_header(key, value)
        self.send_header("X-Cache", cache)
        self.send_header("Age", str(age))
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        if body:
            self.wfile.write(body)


def serve(handler, host, port):
    server = QueuedThreadingHTTPServer((host, port), handler)
    thread = threading.Thread(target=server.serve_forever, daemon=True)
    thread.start()
    return server


def main() -> None:
    global POINT_INTERVAL_S, EVENTS_TTL_S, START
    parser = argparse.ArgumentParser()
    parser.add_argument("--origin-port", type=int, default=18080)
    parser.add_argument("--edge-port", type=int, default=18081)
    parser.add_argument("--upstream-host", default="127.0.0.1")
    parser.add_argument("--upstream-port", type=int, default=0)
    parser.add_argument("--point-interval", type=float, default=20.0)
    parser.add_argument("--events-ttl", type=float, default=1.0)
    parser.add_argument("--hold", type=int, default=0)
    args = parser.parse_args()
    POINT_INTERVAL_S = args.point_interval
    EVENTS_TTL_S = max(0.0, args.events_ttl)
    START = time.time()
    origin = None
    if args.upstream_port > 0:
        EdgeHandler.origin_host = args.upstream_host
        EdgeHandler.origin_port = args.upstream_port
    else:
        EdgeHandler.origin_port = args.origin_port
        origin = serve(OriginHandler, "127.0.0.1", args.origin_port)
    edge = serve(EdgeHandler, "127.0.0.1", args.edge_port)
    if args.hold <= 0:
        try:
            while True:
                time.sleep(1)
        except KeyboardInterrupt:
            pass
    else:
        time.sleep(args.hold)
    if origin is not None:
        origin.shutdown()
    edge.shutdown()


if __name__ == "__main__":
    main()
