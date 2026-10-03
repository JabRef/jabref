#!/usr/bin/env python3
"""End-to-end test for the merged native-messaging host.

Spawns the host as a real subprocess and plays *both* peers the host talks to:

  - the **browser**, over native-messaging stdio (4-byte-len + JSON frames on
    the host's stdin/stdout), and
  - the **extension**, replying to each fetchFulltext / openMathSciNet request.

Then it acts as **JabRef**: reads the discovery file the host drops, pulls the
port + bearer token, and drives the loopback HTTP protocol, asserting status
codes and bodies. Finally it closes stdin (EOF) and checks the host shuts down
and removes its discovery file.

One harness, both hosts: the Python host runs here; the PowerShell host runs
wherever `pwsh` exists. Same asserts for both ⇒ protocol parity.

    python3 e2e_test.py
"""
import http.client, json, os, shutil, struct, subprocess, sys, tempfile, threading, time
from pathlib import Path

HERE = Path(__file__).resolve().parent


# ---- native-messaging framing (what the browser speaks to the host) ----
def read_exact(stream, n):
    buf = b""
    while len(buf) < n:
        chunk = stream.read(n - len(buf))
        if not chunk:
            return None
        buf += chunk
    return buf

def read_frame(stream):
    head = read_exact(stream, 4)
    if head is None:
        return None
    (n,) = struct.unpack("=I", head)
    body = read_exact(stream, n)
    return None if body is None else json.loads(body.decode("utf-8"))

def write_frame(stream, obj):
    data = json.dumps(obj).encode("utf-8")
    stream.write(struct.pack("=I", len(data)) + data)
    stream.flush()


class HostRun:
    """A running host subprocess + the browser/extension standing in for it."""
    def __init__(self, name, argv, pdf_path):
        self.name, self.pdf = name, pdf_path
        self.config = Path(tempfile.mkdtemp(prefix=f"jabext-{name}-"))
        env = {**os.environ, "JABEXT_CONFIG_BASE": str(self.config)}
        self.proc = subprocess.Popen(argv, cwd=HERE, env=env,
                                     stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                                     stderr=subprocess.PIPE)
        self.stderr = []
        threading.Thread(target=self._drain_stderr, daemon=True).start()
        threading.Thread(target=self._extension_loop, daemon=True).start()

    def _drain_stderr(self):
        for line in self.proc.stderr:
            self.stderr.append(line.decode("utf-8", "replace").rstrip())

    def _extension_loop(self):
        """Reply to the host's NM requests exactly as the browser extension would."""
        while True:
            try:
                msg = read_frame(self.proc.stdout)
            except (ValueError, OSError):
                break
            if msg is None:
                break
            rid = msg.get("requestId")
            if msg.get("type") == "fetchFulltext":
                target = (msg.get("doi") or "") + (msg.get("url") or "")
                if "fail" in target:
                    reply = {"requestId": rid, "error": "no-adapter", "message": "no adapter for host"}
                else:
                    reply = {"requestId": rid, "id": "e1", "path": str(self.pdf),
                             "sourceUrl": "https://example.org/paper.pdf"}
            elif msg.get("type") == "openMathSciNet":
                reply = {"requestId": rid, "action": "opened", "tabId": 7}
            else:
                continue
            try:
                write_frame(self.proc.stdin, reply)
            except (OSError, ValueError):
                break

    def discovery(self):
        d_dir = self.config / "fulltext-providers"
        for _ in range(100):                          # up to ~10s (pwsh cold start)
            # Per-instance filename: jabext-bridge.<port>.json (one host per browser).
            matches = sorted(d_dir.glob("jabext-bridge.*.json")) if d_dir.is_dir() else []
            if matches and matches[0].stat().st_size > 0:
                f = matches[0]
                d = json.loads(f.read_text("utf-8"))
                d["_token"] = Path(d["tokenFile"]).read_text("utf-8").strip()
                d["_file"] = f
                return d
            if self.proc.poll() is not None:
                raise RuntimeError(f"host exited early ({self.proc.returncode})\n" + "\n".join(self.stderr))
            time.sleep(0.1)
        raise TimeoutError("discovery file never appeared\n" + "\n".join(self.stderr))

    def close(self):
        try:
            self.proc.stdin.close()                   # EOF -> host shuts down
        except OSError:
            pass
        try:
            return self.proc.wait(timeout=10)
        except subprocess.TimeoutExpired:
            self.proc.kill()
            return "killed"


def httpreq(port, method, path, body=None, headers=None):
    c = http.client.HTTPConnection("127.0.0.1", port, timeout=10)
    c.request(method, path, json.dumps(body) if body is not None else None, headers or {})
    r = c.getresponse()
    data = r.read()
    c.close()
    return r.status, (json.loads(data) if data else {})


def check(host_name, argv):
    pdf = HERE / "e2e.pdf"
    pdf.write_bytes(b"%PDF-1.4\n%%EOF\n")
    run = HostRun(host_name, argv, pdf)
    try:
        disc = run.discovery()
        port, token = disc["port"], disc["_token"]
        assert disc["name"] == "jabext-bridge" and disc["protocolVersion"] == 1, disc
        auth = {"Authorization": f"Bearer {token}"}

        # health (spec: bearer required, same as every endpoint)
        s, _ = httpreq(port, "GET", "/v1/health")
        assert s == 401, ("health without token -> 401", s)
        s, b = httpreq(port, "GET", "/v1/health", None, auth)
        assert s == 200 and b["ok"] is True and b["name"] == "jabext-bridge", (s, b)

        # auth + origin gating
        s, _ = httpreq(port, "POST", "/v1/fulltext", {"doi": "10/x"})
        assert s == 401, f"missing token -> {s}"
        s, _ = httpreq(port, "POST", "/v1/fulltext", {"doi": "10/x"}, {**auth, "Origin": "https://evil.example"})
        assert s == 403, f"browser origin -> {s}"

        # validation
        s, _ = httpreq(port, "POST", "/v1/fulltext", {}, auth)
        assert s == 400, f"no doi/url -> {s}"

        # happy path: full HTTP -> NM -> extension -> NM -> HTTP round trip
        s, b = httpreq(port, "POST", "/v1/fulltext", {"doi": "10.1/abc"}, auth)
        assert s == 200 and b["path"] == str(pdf) and b["sourceUrl"], (s, b)

        # error propagation from the extension
        s, b = httpreq(port, "POST", "/v1/fulltext", {"url": "https://fail.example/x"}, auth)
        assert s == 404 and b["error"] == "no-adapter", (s, b)

        # mathscinet
        s, b = httpreq(port, "POST", "/v1/mathscinet/open", {"mrNumber": "MR12345"}, auth)
        assert s == 200 and b["action"] == "opened" and b["tabId"] == 7, (s, b)
        s, _ = httpreq(port, "POST", "/v1/mathscinet/open", {}, auth)
        assert s == 400, f"no mrNumber -> {s}"

        # clean shutdown on stdin EOF + discovery cleanup
        rc = run.close()
        assert rc == 0, f"exit code {rc}\n" + "\n".join(run.stderr)
        assert not disc["_file"].exists(), "discovery file not cleaned up"
        print(f"  PASS {host_name}: 9 assertions (health, auth, origin, validation, "
              f"fetch, error-prop, mathscinet, shutdown, cleanup)")
        return True
    except Exception as e:
        print(f"  FAIL {host_name}: {e}")
        if run.stderr:
            print("    host stderr:\n      " + "\n      ".join(run.stderr[-15:]))
        return False
    finally:
        run.close()
        shutil.rmtree(run.config, ignore_errors=True)
        pdf.unlink(missing_ok=True)


def main():
    hosts = [("python", [sys.executable, str(HERE / "jabext_host.py")])]
    pwsh = shutil.which("pwsh") or shutil.which("powershell")
    if pwsh:
        hosts.append(("powershell", [pwsh, "-NoProfile", "-File", str(HERE / "jabext_host.ps1")]))
    else:
        print("  SKIP powershell: pwsh not on PATH (PS1 runs the same asserts where it is)")

    print("e2e: driving host subprocess as browser + extension + JabRef")
    ok = all(check(name, argv) for name, argv in hosts)
    print("OK" if ok else "FAILURES")
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
