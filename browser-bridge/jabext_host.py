#!/usr/bin/env python3
"""Merged JabRef native-messaging host — sketch.

Folds the browser-extension *fulltext bridge* (JabExtBridge.java, req~bxf.*~1)
into the existing Python NM host (jabrefHost.py). One process the browser
launches via connectNative; it serves the loopback HTTP protocol AND (where
noted) the existing import-to-JabRef stdio commands.

Ships as source like jabrefHost.py: no GraalVM, no JBang, no mise, no per-OS
binary to build/bundle. Stdlib only.

Run the self-check (no browser needed):  python3 jabext_host.py --selftest
"""
import json, os, secrets, struct, sys, threading, platform
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

PROVIDER_NAME = "jabext-bridge"
PROVIDER_DISPLAY_NAME = "JabRef Browser Extension (experimental)"
PROTOCOL_VERSION = 1
FETCH_TIMEOUT = 300.0            # seconds; req~bxf.sync-hold / cancellation fallback
MATHSCINET_TIMEOUT = 10.0
MAX_NM_MESSAGE = 1 << 20

# error-code -> HTTP status (mirrors JabExtBridge.httpStatusForError)
_STATUS = {"no-pdf-found": 404, "no-adapter": 404, "auth-required": 404,
           "not-reachable": 404, "timeout": 504, "busy": 503, "bad-request": 400}


# ---- platform paths (mirror BrowserExtensionProviderDiscovery) ----
def _config_base() -> Path:
    override = os.environ.get("JABEXT_CONFIG_BASE")   # test/CI hook
    if override:
        return Path(override)
    system = platform.system()
    if system == "Windows":
        return Path(os.environ.get("APPDATA") or Path.home() / "AppData/Roaming") / "JabRef"
    if system == "Darwin":
        return Path.home() / "Library/Application Support/JabRef"
    return Path(os.environ.get("XDG_CONFIG_HOME") or Path.home() / ".config") / "jabref"

DISCOVERY_DIR = _config_base() / "fulltext-providers"
TOKEN_DIR = _config_base() / "fulltext-providers-state"


def ensure_token() -> tuple[Path, str]:
    TOKEN_DIR.mkdir(parents=True, exist_ok=True)
    f = TOKEN_DIR / f"{PROVIDER_NAME}.token"
    if f.exists() and f.stat().st_size > 0:
        return f, f.read_text("utf-8").strip()
    token = secrets.token_urlsafe(32)
    f.write_text(token + "\n", "utf-8")
    try:
        os.chmod(f, 0o600)                       # POSIX; Windows inherits user-only ACL
    except OSError:
        pass
    return f, token


# ---- native-messaging stdio (same framing as jabrefHost.py) ----
_stdout_lock = threading.Lock()

def write_frame(obj: dict, out=None) -> None:
    out = out or sys.stdout.buffer
    data = json.dumps(obj).encode("utf-8")
    if len(data) > MAX_NM_MESSAGE:
        raise ValueError("NM payload exceeds 1 MiB")
    with _stdout_lock:
        out.write(struct.pack("=I", len(data)))  # native byte order, per NM spec
        out.write(data)
        out.flush()

def read_frame(inp) -> dict | None:
    head = inp.read(4)
    if len(head) < 4:
        return None
    (n,) = struct.unpack("=I", head)
    if n <= 0 or n > MAX_NM_MESSAGE:
        return None
    body = inp.read(n)
    return json.loads(body.decode("utf-8")) if len(body) == n else None


# ---- request correlation: HTTP thread <-> extension reply ----
class Pending:
    def __init__(self):
        self._slots: dict[str, tuple[threading.Event, list]] = {}
        self._seq = 0
        self._lock = threading.Lock()

    def new(self) -> tuple[str, threading.Event, list]:
        with self._lock:
            self._seq += 1
            rid = f"r{self._seq}"
            ev, box = threading.Event(), []
            self._slots[rid] = (ev, box)
        return rid, ev, box

    def deliver(self, reply: dict) -> None:
        rid = reply.get("requestId")
        with self._lock:
            slot = self._slots.get(rid)
        if slot:
            slot[1].append(reply)
            slot[0].set()

    def drop(self, rid: str) -> None:
        with self._lock:
            self._slots.pop(rid, None)

PENDING = Pending()
BEARER = ""
# send_nm is pluggable so the self-check can stand in for the extension.
send_nm = write_frame


def _round_trip(nm_msg: dict, timeout: float) -> dict:
    rid, ev, box = PENDING.new()
    nm_msg["requestId"] = rid
    try:
        send_nm(nm_msg)
        if not ev.wait(timeout):
            return {"error": "timeout", "message": "provider fetch exceeded internal timeout"}
        return box[0]
    finally:
        PENDING.drop(rid)


# ---- HTTP server (req~bxf.*) ----
class Handler(BaseHTTPRequestHandler):
    def log_message(self, *a):                   # keep stdout clean for NM frames
        pass

    def _reject_origin(self) -> bool:
        origin = self.headers.get("Origin")
        if origin in (None, "", "null"):
            return False
        self._error(403, "bad-request", "Origin header rejected")
        return True

    def _reject_bearer(self) -> bool:
        auth = self.headers.get("Authorization", "")
        if not auth.startswith("Bearer ") or not secrets.compare_digest(auth[7:].strip(), BEARER):
            self._error(401, "bad-request", "Missing or invalid bearer token")
            return True
        return False

    def _body(self) -> dict:
        n = int(self.headers.get("Content-Length") or 0)
        return json.loads(self.rfile.read(n) or b"{}")

    def _json(self, status: int, obj: dict) -> None:
        data = json.dumps(obj).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def _error(self, status: int, code: str, msg: str) -> None:
        self._json(status, {"error": code, "message": msg})

    def do_GET(self):
        if self.path != "/v1/health":
            return self._error(404, "bad-request", "unknown endpoint")
        if self._reject_origin():
            return
        self._json(200, {"ok": True, "name": PROVIDER_NAME, "protocolVersion": PROTOCOL_VERSION})

    def do_POST(self):
        if self._reject_origin() or self._reject_bearer():
            return
        try:
            body = self._body()
        except ValueError:
            return self._error(400, "bad-request", "Malformed request body")

        if self.path == "/v1/fulltext":
            if not (body.get("doi") or body.get("url")):
                return self._error(400, "bad-request", "At least one of doi or url is required")
            r = _round_trip({"type": "fetchFulltext", "doi": body.get("doi"), "url": body.get("url")},
                            FETCH_TIMEOUT)
            if r.get("error"):
                return self._error(_STATUS.get(r["error"], 500), r["error"], r.get("message", r["error"]))
            if not r.get("path") or not Path(r["path"]).is_file():
                return self._error(404, "no-pdf-found", "Provider returned no readable PDF path")
            out = {"id": r.get("id"), "path": r["path"]}
            if r.get("sourceUrl"):
                out["sourceUrl"] = r["sourceUrl"]
            return self._json(200, out)

        if self.path == "/v1/mathscinet/open":
            if not body.get("mrNumber"):
                return self._error(400, "bad-request", "mrNumber is required")
            r = _round_trip({"type": "openMathSciNet", "mrNumber": body["mrNumber"]}, MATHSCINET_TIMEOUT)
            if r.get("error"):
                return self._error(_STATUS.get(r["error"], 500), r["error"], r.get("message", r["error"]))
            if not r.get("action") or r.get("tabId") is None:
                return self._error(500, "internal-error", "Provider returned no tab action")
            return self._json(200, {"action": r["action"], "tabId": r["tabId"]})

        self._error(404, "bad-request", "unknown endpoint")


def start_http() -> ThreadingHTTPServer:
    srv = ThreadingHTTPServer(("127.0.0.1", 0), Handler)
    threading.Thread(target=srv.serve_forever, daemon=True).start()
    return srv

def write_discovery(port: int, token_file: Path) -> Path:
    DISCOVERY_DIR.mkdir(parents=True, exist_ok=True)
    f = DISCOVERY_DIR / f"{PROVIDER_NAME}.json"
    f.write_text(json.dumps({
        "name": PROVIDER_NAME, "displayName": PROVIDER_DISPLAY_NAME,
        "port": port, "tokenFile": str(token_file), "protocolVersion": PROTOCOL_VERSION,
    }), "utf-8")
    return f


def nm_loop() -> None:
    """Main read loop. Extension messages are either replies to pending HTTP
    requests, or the existing jabrefHost import commands."""
    while True:
        msg = read_frame(sys.stdin.buffer)
        if msg is None:
            break                                # stdin EOF: browser/extension gone
        if "requestId" in msg:
            PENDING.deliver(msg)
        else:
            handle_import_message(msg)           # <-- existing jabrefHost.py logic lives here

def handle_import_message(msg: dict) -> None:
    # Placeholder for jabrefHost.py's add_jabref_entry()/subprocess dispatch.
    pass


def main() -> int:
    global BEARER
    token_file, BEARER = ensure_token()
    srv = start_http()
    discovery = write_discovery(srv.server_address[1], token_file)
    try:
        nm_loop()
    finally:
        try: discovery.unlink()
        except OSError: pass
        srv.shutdown()
    return 0


# ---- self-check: stands in for the browser + extension ----
def _selftest() -> None:
    import http.client, os as _os, tempfile
    global BEARER, send_nm
    BEARER = "test-token"
    srv = start_http()
    port = srv.server_address[1]

    # Fake extension: whatever the host "sends", reply after a beat.
    pdf = Path(tempfile.gettempdir()) / "selftest.pdf"; pdf.write_bytes(b"%PDF-1.4\n")
    def fake_send(msg):
        def reply():
            if msg["type"] == "fetchFulltext":
                PENDING.deliver({"requestId": msg["requestId"], "id": "e1",
                                 "path": str(pdf), "sourceUrl": "https://x/y.pdf"})
            else:
                PENDING.deliver({"requestId": msg["requestId"], "action": "opened", "tabId": 7})
        threading.Timer(0.05, reply).start()
    send_nm = fake_send

    def req(method, path, body=None, headers=None):
        c = http.client.HTTPConnection("127.0.0.1", port, timeout=5)
        c.request(method, path, json.dumps(body) if body is not None else None, headers or {})
        r = c.getresponse(); return r.status, json.loads(r.read() or b"{}")

    auth = {"Authorization": f"Bearer {BEARER}"}
    s, b = req("GET", "/v1/health");                         assert (s, b["ok"]) == (200, True), b
    s, _ = req("POST", "/v1/fulltext", {"doi": "10/x"});     assert s == 401, "no token -> 401"
    s, _ = req("POST", "/v1/fulltext", {"doi": "10/x"}, {**auth, "Origin": "https://evil"}); assert s == 403
    s, _ = req("POST", "/v1/fulltext", {}, auth);            assert s == 400, "no doi/url -> 400"
    s, b = req("POST", "/v1/fulltext", {"doi": "10/x"}, auth); assert (s, b["path"]) == (200, str(pdf)), b
    s, b = req("POST", "/v1/mathscinet/open", {"mrNumber": "MR123"}, auth); assert (s, b["tabId"]) == (200, 7), b
    _os.chmod  # touch import so linters stay quiet
    pdf.unlink()
    srv.shutdown()
    print("selftest OK: health, auth+origin gating, fulltext correlation, mathscinet")


if __name__ == "__main__":
    sys.exit(_selftest() if "--selftest" in sys.argv else main())
