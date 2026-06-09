#!/usr/bin/env python3
"""Check availability of the JabRef HTTP (REST) server.

Mirrors the most basic commands of jabsrv/src/test/rest-api.http, but uses only
the Python standard library (urllib) so it runs without IntelliJ's HTTP Client
and without extra dependencies.

Start the server first using Gradle:
    Gradle > JabRef > jabsrv-cli > Tasks > application > run

Usage:
    python scripts/check-http-server.py
    python scripts/check-http-server.py --base-url http://localhost:23119
    python scripts/check-http-server.py --timeout 5

Exit code 0 if the server is reachable and both checks pass, 1 otherwise.
"""

import argparse
import sys
import urllib.error
import urllib.request

DEFAULT_BASE_URL = "http://localhost:23119"

# ANSI colors (best effort; harmless if the terminal ignores them)
OK = "\033[0;32m"
WARN = "\033[0;33m"
ERROR = "\033[0;31m"
ENDC = "\033[0m"


def request(base_url, path, accept=None, timeout=5):
    """Perform one GET request. Returns (status, body_bytes) or raises."""
    req = urllib.request.Request(base_url.rstrip("/") + path)
    if accept:
        req.add_header("Accept", accept)
    with urllib.request.urlopen(req, timeout=timeout) as resp:
        return resp.status, resp.read()


def check(name, base_url, path, timeout, accept=None):
    """Run one check, print result, return True on pass."""
    try:
        status, payload = request(base_url, path, accept, timeout)
    except urllib.error.HTTPError as e:
        status, payload = e.code, e.read()
    except (urllib.error.URLError, OSError) as e:
        print("{}[FAIL]{} {} -> {}".format(ERROR, ENDC, name, e))
        return False

    if status == 200:
        snippet = payload[:60].decode("utf-8", "replace").replace("\n", " ").strip()
        print("{}[ OK ]{} {} (200) {}".format(OK, ENDC, name, snippet))
        return True

    print("{}[FAIL]{} {} (got {}, expected 200)".format(ERROR, ENDC, name, status))
    return False


def main():
    parser = argparse.ArgumentParser(description="Check JabRef HTTP server availability.")
    parser.add_argument("--base-url", default=DEFAULT_BASE_URL,
                        help="Base URL of the server (default: %(default)s)")
    parser.add_argument("--timeout", type=float, default=5.0,
                        help="Per-request timeout in seconds (default: %(default)s)")
    args = parser.parse_args()

    base, t = args.base_url, args.timeout
    print("Checking JabRef HTTP server at {} ...".format(base))

    ok_root = check("Root page", base, "/", t)
    ok_libs = check("List libraries", base, "/libraries", t, accept="application/json")

    if ok_root and ok_libs:
        print("{}Server is up.{}".format(OK, ENDC))
        return 0

    print("{}Hint:{} start it via Gradle > JabRef > jabsrv-cli > Tasks > application > run"
          .format(WARN, ENDC))
    return 1


if __name__ == "__main__":
    sys.exit(main())
