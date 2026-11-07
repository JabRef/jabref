#!/usr/bin/env python3

"""Run a protocol-level smoke test against the JabLS native executable."""

from __future__ import annotations

import argparse
import json
import socket
import subprocess
import sys
import tempfile
import time
from dataclasses import dataclass
from pathlib import Path
from typing import Callable


JsonObject = dict[str, object]
MessagePredicate = Callable[[JsonObject], bool]


class SmokeTestFailure(RuntimeError):
    """Raised when the native server breaks the smoke-test contract."""


@dataclass(frozen=True)
class SmokeFixtures:
    diagnostic_path: Path
    diagnostic_bibtex: str
    changed_diagnostic_bibtex: str
    definition_path: Path
    definition_bibtex: str
    definition_character: int
    pdf_path: Path


class LspConnection:
    """Send and receive Content-Length framed JSON-RPC messages."""

    def __init__(self, connection: socket.socket, timeout: float) -> None:
        self.connection = connection
        self.timeout = timeout
        self.buffer = bytearray()
        self.pending_messages: list[JsonObject] = []
        self.transcript: list[JsonObject] = []

    def close(self) -> None:
        self.connection.close()

    def send(self, message: JsonObject) -> None:
        payload = json.dumps(message, separators=(",", ":"), ensure_ascii=False).encode(
            "utf-8"
        )
        header = f"Content-Length: {len(payload)}\r\n\r\n".encode("ascii")
        self.connection.sendall(header + payload)

    def wait_for_response(self, request_id: int) -> JsonObject:
        return self._wait_for(
            lambda message: message.get("id") == request_id,
            f"response with id {request_id}",
        )

    def wait_for_notification(
        self, method: str, predicate: MessagePredicate | None = None
    ) -> JsonObject:
        def matches(message: JsonObject) -> bool:
            return message.get("method") == method and (
                predicate is None or predicate(message)
            )

        return self._wait_for(matches, f"notification {method}")

    def _wait_for(self, predicate: MessagePredicate, description: str) -> JsonObject:
        deadline = time.monotonic() + self.timeout

        while True:
            for index, message in enumerate(self.pending_messages):
                if predicate(message):
                    return self.pending_messages.pop(index)

            remaining = deadline - time.monotonic()
            if remaining <= 0:
                raise SmokeTestFailure(f"Timed out waiting for {description}")

            message = self._read_message(remaining)
            self.transcript.append(message)
            if "error" in message:
                raise SmokeTestFailure(
                    "JabLS returned a JSON-RPC error:\n"
                    + json.dumps(message, indent=2, ensure_ascii=False)
                )
            if predicate(message):
                return message
            self.pending_messages.append(message)

    def _read_message(self, timeout: float) -> JsonObject:
        header_separator = b"\r\n\r\n"
        self.connection.settimeout(timeout)

        try:
            while header_separator not in self.buffer:
                self._receive_more()

            header_end = self.buffer.index(header_separator)
            raw_headers = bytes(self.buffer[:header_end]).decode("ascii")
            content_length = self._content_length(raw_headers)
            payload_start = header_end + len(header_separator)
            payload_end = payload_start + content_length

            while len(self.buffer) < payload_end:
                self._receive_more()

            payload = bytes(self.buffer[payload_start:payload_end])
            del self.buffer[:payload_end]
        except TimeoutError as error:
            raise SmokeTestFailure("Timed out while reading an LSP message") from error

        decoded = json.loads(payload.decode("utf-8"))
        if not isinstance(decoded, dict):
            raise SmokeTestFailure(
                f"Expected a JSON object, got {type(decoded).__name__}"
            )
        return decoded

    def _receive_more(self) -> None:
        chunk = self.connection.recv(64 * 1024)
        if not chunk:
            raise SmokeTestFailure("JabLS closed the TCP connection unexpectedly")
        self.buffer.extend(chunk)

    @staticmethod
    def _content_length(raw_headers: str) -> int:
        for header in raw_headers.split("\r\n"):
            name, separator, value = header.partition(":")
            if separator and name.lower() == "content-length":
                try:
                    return int(value.strip())
                except ValueError as error:
                    raise SmokeTestFailure(
                        f"Invalid Content-Length header: {header}"
                    ) from error
        raise SmokeTestFailure(
            f"LSP message has no Content-Length header: {raw_headers}"
        )


def find_repository_root(script_path: Path) -> Path:
    for candidate in script_path.parents:
        if (candidate / "settings.gradle.kts").is_file():
            return candidate
    raise SmokeTestFailure("Could not find the JabRef repository root")


def default_binary(repository_root: Path) -> Path:
    binary = (
        repository_root
        / "jabls-cli"
        / "build"
        / "native"
        / "nativeCompile"
        / "jabls-cli"
    )
    windows_binary = binary.with_suffix(".exe")
    return windows_binary if windows_binary.is_file() else binary


# Retries guard against the picked port being taken before JabLS can bind it.
MAX_STARTUP_ATTEMPTS = 3


def find_available_port() -> int:
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as probe:
        probe.bind(("127.0.0.1", 0))
        return int(probe.getsockname()[1])


def connect_to_server(
    process: subprocess.Popen[bytes], port: int, timeout: float
) -> socket.socket:
    deadline = time.monotonic() + timeout
    last_error: OSError | None = None

    while time.monotonic() < deadline:
        exit_code = process.poll()
        if exit_code is not None:
            raise SmokeTestFailure(
                f"JabLS exited before accepting connections (exit code {exit_code})"
            )

        try:
            return socket.create_connection(("127.0.0.1", port), timeout=0.25)
        except OSError as error:
            last_error = error
            time.sleep(0.1)

    raise SmokeTestFailure(f"JabLS did not listen on port {port}: {last_error}")


def terminate_process(process: subprocess.Popen[bytes]) -> None:
    if process.poll() is not None:
        return
    process.terminate()
    try:
        process.wait(timeout=2)
    except subprocess.TimeoutExpired:
        process.kill()
        process.wait(timeout=2)


def write_fixtures(work_directory: Path) -> SmokeFixtures:
    pdf_path = work_directory / "test.pdf"
    pdf_path.write_bytes(b"%PDF-1.4\n% JabLS native smoke-test fixture\n")

    diagnostic_bibtex = """\
@misc{WrongKey,
  author = {Knuth},
  year   = {2014}
}
"""
    changed_diagnostic_bibtex = diagnostic_bibtex.replace(
        "@misc{WrongKey,", "@misc{Knuth2014,"
    )
    diagnostic_path = work_directory / "diagnostic.bib"
    diagnostic_path.write_text(diagnostic_bibtex, encoding="utf-8")

    file_line = f"  file         = {{:{pdf_path}:PDF}}"
    definition_bibtex = "\n".join(
        (
            "@misc{DefinitionFixture,",
            file_line,
            "}",
            "",
        )
    )
    definition_path = work_directory / "definition.bib"
    definition_path.write_text(definition_bibtex, encoding="utf-8")
    position_in_file_path = file_line.index(str(pdf_path)) + max(
        1, len(str(pdf_path)) // 2
    )
    return SmokeFixtures(
        diagnostic_path=diagnostic_path,
        diagnostic_bibtex=diagnostic_bibtex,
        changed_diagnostic_bibtex=changed_diagnostic_bibtex,
        definition_path=definition_path,
        definition_bibtex=definition_bibtex,
        definition_character=position_in_file_path,
        pdf_path=pdf_path,
    )


def diagnostic_version_is(version: int) -> MessagePredicate:
    def matches(message: JsonObject) -> bool:
        params = message.get("params")
        return isinstance(params, dict) and params.get("version") == version

    return matches


def require_result(response: JsonObject, request_id: int) -> object:
    if response.get("id") != request_id or "result" not in response:
        raise SmokeTestFailure(
            f"Response {request_id} has no result:\n"
            + json.dumps(response, indent=2, ensure_ascii=False)
        )
    return response["result"]


def run_protocol_smoke_test(connection: LspConnection, work_directory: Path) -> None:
    fixtures = write_fixtures(work_directory)
    diagnostic_uri = fixtures.diagnostic_path.as_uri()
    definition_uri = fixtures.definition_path.as_uri()
    pdf_uri = fixtures.pdf_path.as_uri()

    # 3. Say hello and check that JabLS advertises the core features used below.
    connection.send(
        {
            "jsonrpc": "2.0",
            "id": 1,
            "method": "initialize",
            "params": {"processId": None, "rootUri": None, "capabilities": {}},
        }
    )
    initialize_result = require_result(connection.wait_for_response(1), 1)
    if not isinstance(initialize_result, dict):
        raise SmokeTestFailure("initialize result is not a JSON object")
    capabilities = initialize_result.get("capabilities")
    if not isinstance(capabilities, dict):
        raise SmokeTestFailure("initialize result has no capabilities object")
    if (
        "textDocumentSync" not in capabilities
        or capabilities.get("definitionProvider") is not True
    ):
        raise SmokeTestFailure(
            f"JabLS returned unexpected capabilities: {capabilities}"
        )
    print("PASS  initialize returned text-document sync and definition support")

    # This message travels the other way through the LanguageClient proxy.
    connection.wait_for_notification(
        "window/logMessage",
        lambda message: isinstance(message.get("params"), dict)
        and message["params"].get("message") == "BibtexLSPServer connected.",
    )
    print("PASS  the server-to-client LanguageClient proxy delivered logMessage")

    # 4. Tell JabLS that the client has finished its side of the LSP handshake.
    connection.send({"jsonrpc": "2.0", "method": "initialized", "params": {}})
    print("PASS  initialized notification was sent")

    # 5. Open the exact Misc entry used by CitationKeyDeviationCheckerTest.
    connection.send(
        {
            "jsonrpc": "2.0",
            "method": "textDocument/didOpen",
            "params": {
                "textDocument": {
                    "uri": diagnostic_uri,
                    "languageId": "bibtex",
                    "version": 1,
                    "text": fixtures.diagnostic_bibtex,
                }
            },
        }
    )
    # 6. Wait for JabLS to run the jablib integrity checks and send the result back.
    diagnostics_message = connection.wait_for_notification(
        "textDocument/publishDiagnostics",
        diagnostic_version_is(1),
    )
    diagnostics_params = diagnostics_message.get("params")
    diagnostics = (
        diagnostics_params.get("diagnostics")
        if isinstance(diagnostics_params, dict)
        else None
    )
    if not isinstance(diagnostics, list) or not diagnostics:
        raise SmokeTestFailure("didOpen did not publish any diagnostics")
    diagnostic_messages = [
        diagnostic.get("message", "")
        for diagnostic in diagnostics
        if isinstance(diagnostic, dict)
    ]
    expected_diagnostic = "Citation key deviates from generated key Knuth2014"
    if expected_diagnostic not in diagnostic_messages:
        raise SmokeTestFailure(
            "didOpen did not publish the citation-key diagnostic. Received:\n"
            + json.dumps(diagnostics, indent=2, ensure_ascii=False)
        )
    print("PASS  didOpen published the jablib citation-key diagnostic")

    # 7. Open a separate file fixture, then ask JabLS to resolve its linked PDF.
    connection.send(
        {
            "jsonrpc": "2.0",
            "method": "textDocument/didOpen",
            "params": {
                "textDocument": {
                    "uri": definition_uri,
                    "languageId": "bibtex",
                    "version": 10,
                    "text": fixtures.definition_bibtex,
                }
            },
        }
    )
    connection.wait_for_notification(
        "textDocument/publishDiagnostics",
        diagnostic_version_is(10),
    )
    connection.send(
        {
            "jsonrpc": "2.0",
            "id": 2,
            "method": "textDocument/definition",
            "params": {
                "textDocument": {"uri": definition_uri},
                "position": {
                    "line": 1,
                    "character": fixtures.definition_character,
                },
            },
        }
    )
    definition_result = require_result(connection.wait_for_response(2), 2)
    if not isinstance(definition_result, list) or not any(
        isinstance(location, dict) and location.get("uri") == pdf_uri
        for location in definition_result
    ):
        raise SmokeTestFailure(
            "definition did not resolve the linked PDF. Received:\n"
            + json.dumps(definition_result, indent=2, ensure_ascii=False)
        )
    print("PASS  definition resolved the linked PDF")

    # 8. Disable both checks, matching ExtensionSettingsTest.copyFromJsonObject().
    connection.send(
        {
            "jsonrpc": "2.0",
            "method": "workspace/didChangeConfiguration",
            "params": {
                "settings": {
                    "jabref": {
                        "consistencyCheck": {
                            "enabled": False,
                            "required": False,
                            "optional": False,
                            "unknown": False,
                        },
                        "integrityCheck": {"enabled": False},
                    }
                }
            },
        }
    )
    refreshed_diagnostics = connection.wait_for_notification(
        "textDocument/publishDiagnostics",
        diagnostic_version_is(-1),
    )
    refreshed_params = refreshed_diagnostics.get("params")
    if (
        not isinstance(refreshed_params, dict)
        or refreshed_params.get("diagnostics") != []
    ):
        raise SmokeTestFailure(
            "didChangeConfiguration did not disable diagnostics. Received:\n"
            + json.dumps(refreshed_diagnostics, indent=2, ensure_ascii=False)
        )
    print("PASS  didChangeConfiguration refreshed diagnostics")

    # 9. Send the full updated document and check that JabLS publishes version 2.
    connection.send(
        {
            "jsonrpc": "2.0",
            "method": "textDocument/didChange",
            "params": {
                "textDocument": {"uri": diagnostic_uri, "version": 2},
                "contentChanges": [{"text": fixtures.changed_diagnostic_bibtex}],
            },
        }
    )
    changed_diagnostics = connection.wait_for_notification(
        "textDocument/publishDiagnostics",
        diagnostic_version_is(2),
    )
    changed_params = changed_diagnostics.get("params")
    if not isinstance(changed_params, dict) or not isinstance(
        changed_params.get("diagnostics"), list
    ):
        raise SmokeTestFailure("didChange returned malformed diagnostics")
    print("PASS  didChange published diagnostics for document version 2")

    # 10. Ask for a graceful shutdown and wait for the required null response.
    connection.send(
        {
            "jsonrpc": "2.0",
            "id": 99,
            "method": "shutdown",
            "params": None,
        }
    )
    shutdown_result = require_result(connection.wait_for_response(99), 99)
    if shutdown_result is not None:
        raise SmokeTestFailure(f"shutdown returned {shutdown_result!r}, expected null")
    print("PASS  shutdown returned result: null")

    # 11. Finish the LSP lifecycle; standalone JabLS should now terminate itself.
    connection.send({"jsonrpc": "2.0", "method": "exit", "params": None})
    print("PASS  exit notification was sent")


def print_debug_information(log_path: Path, connection: LspConnection | None) -> None:
    if connection is not None and connection.transcript:
        print("\nJSON-RPC transcript:", file=sys.stderr)
        for message in connection.transcript:
            print(json.dumps(message, indent=2, ensure_ascii=False), file=sys.stderr)

    if log_path.is_file():
        server_log = log_path.read_text(encoding="utf-8", errors="replace")
        if server_log:
            print("\nJabLS server log:", file=sys.stderr)
            print(server_log, file=sys.stderr)


def parse_arguments(repository_root: Path) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--binary",
        type=Path,
        default=default_binary(repository_root),
        help="Path to the JabLS native executable",
    )
    parser.add_argument("--startup-timeout", type=float, default=20.0)
    parser.add_argument("--message-timeout", type=float, default=10.0)
    parser.add_argument("--exit-timeout", type=float, default=10.0)
    parser.add_argument(
        "--show-server-log",
        action="store_true",
        help="Print the server log even when the smoke test passes",
    )
    return parser.parse_args()


def main() -> int:
    script_path = Path(__file__).resolve()
    repository_root = find_repository_root(script_path)
    arguments = parse_arguments(repository_root)
    binary = arguments.binary.expanduser().resolve()

    if not binary.is_file():
        print(f"Native binary not found: {binary}", file=sys.stderr)
        print(
            "Build it with: ./gradlew :jabls-cli:nativeCompile --no-configuration-cache",
            file=sys.stderr,
        )
        return 1

    connection: LspConnection | None = None
    process: subprocess.Popen[bytes] | None = None
    succeeded = False

    with tempfile.TemporaryDirectory(
        prefix="jabls-native-smoke-"
    ) as temporary_directory:
        work_directory = Path(temporary_directory)
        log_path = work_directory / "jabls.log"

        try:
            with log_path.open("wb") as server_log:
                # 1. Start the native executable on a free port and save its log for failures.
                for attempt in range(1, MAX_STARTUP_ATTEMPTS + 1):
                    port = find_available_port()
                    process = subprocess.Popen(
                        [str(binary), "--port", str(port)],
                        cwd=repository_root,
                        stdout=server_log,
                        stderr=subprocess.STDOUT,
                    )
                    # 2. Retry until JabLS accepts one TCP connection, then keep it open.
                    try:
                        socket_connection = connect_to_server(
                            process, port, arguments.startup_timeout
                        )
                        break
                    except SmokeTestFailure:
                        terminate_process(process)
                        process = None
                        if attempt == MAX_STARTUP_ATTEMPTS:
                            raise

                print(f"PASS  native JabLS started on port {port}")
                connection = LspConnection(socket_connection, arguments.message_timeout)
                print("PASS  connected to JabLS over TCP")

                run_protocol_smoke_test(connection, work_directory)

                # 12. Wait for a clean exit to prove that JabLS handled the exit notification.
                try:
                    exit_code = process.wait(timeout=arguments.exit_timeout)
                except subprocess.TimeoutExpired as error:
                    raise SmokeTestFailure(
                        "JabLS did not exit after the exit notification"
                    ) from error
                if exit_code != 0:
                    raise SmokeTestFailure(
                        f"JabLS exited with code {exit_code}, expected 0"
                    )
                print("PASS  native JabLS exited with code 0")
                succeeded = True
        except (OSError, SmokeTestFailure) as error:
            print(f"\nFAIL  {error}", file=sys.stderr)
        finally:
            if connection is not None:
                connection.close()
            if process is not None:
                terminate_process(process)

        if not succeeded or arguments.show_server_log:
            print_debug_information(log_path, connection)

    if succeeded:
        print("\nJabLS native smoke test passed.")
        return 0
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
