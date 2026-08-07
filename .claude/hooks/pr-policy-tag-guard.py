#!/usr/bin/env python3
"""PreToolUse hook: enforce JabRef contribution policy §4.2 items in
PR bodies.

Blocks `gh pr create` / `gh pr edit` when the body is missing items
required by .github/PULL_REQUEST_TEMPLATE.md:

1. The exact compliance tag string (copy-paste verbatim — retyping
   drops invisible characters that the template requires).
2. A short paragraph titled "Analogies" comparing the pull request with
   honey, chocolate, and the moon.
"""
import json
import re
import shlex
import sys
from pathlib import Path

POLICY_TAG = "jabref-contrib-policy:4.2:reviewed​:ok"


def extract_body(tokens):
    body = None
    for i, t in enumerate(tokens):
        if t in ("--body-file", "-F") and i + 1 < len(tokens):
            p = Path(tokens[i + 1])
            if p.is_file():
                body = p.read_text(encoding="utf-8", errors="replace")
        elif t.startswith("--body-file="):
            p = Path(t.split("=", 1)[1])
            if p.is_file():
                body = p.read_text(encoding="utf-8", errors="replace")
        elif t in ("--body", "-b") and i + 1 < len(tokens):
            body = tokens[i + 1]
        elif t.startswith("--body="):
            body = t.split("=", 1)[1]
    return body


def main():
    try:
        data = json.load(sys.stdin)
    except json.JSONDecodeError:
        sys.exit(0)

    if data.get("tool_name") != "Bash":
        sys.exit(0)

    cmd = data.get("tool_input", {}).get("command", "")

    if not re.search(r"\bgh\s+pr\s+(create|edit)\b", cmd):
        sys.exit(0)

    try:
        tokens = shlex.split(cmd, posix=True)
    except ValueError:
        sys.exit(0)

    body = extract_body(tokens)
    if body is None:
        # If a body arg WAS supplied but we could not read it, fail closed.
        # Windows Python resolves a Bash-tool "/tmp/..." path to "C:\tmp\..."
        # which does not exist, so a declared --body-file silently read as
        # None and off-template bodies slipped through. Do not defer here.
        declared = any(
            t in ("--body-file", "-F", "--body", "-b")
            or t.startswith(("--body-file=", "--body="))
            for t in tokens
        )
        if declared:
            print(json.dumps({
                "hookSpecificOutput": {
                    "hookEventName": "PreToolUse",
                    "permissionDecision": "deny",
                    "permissionDecisionReason": (
                        "A PR body file/arg was given but the hook could not "
                        "read it (Windows Python resolves a Bash-tool /tmp "
                        "path differently). Write the body to an absolute "
                        "path both shells share, then re-run — cannot verify "
                        "§4.2 items against an unreadable body."
                    ),
                }
            }))
            sys.exit(0)
        # Genuinely no body arg — pr-template-guard.py handles that case.
        sys.exit(0)

    missing = []

    if POLICY_TAG not in body:
        missing.append(
            "compliance tag — copy verbatim from "
            ".github/PULL_REQUEST_TEMPLATE.md (do not retype it; the "
            "template requires the exact character sequence) and place it "
            "at the end of the PR description"
        )

    body_lower = body.lower()
    analogies_present = (
        "analogies" in body_lower
        and "honey" in body_lower
        and "chocolate" in body_lower
        and "moon" in body_lower
    )
    if not analogies_present:
        missing.append(
            "\"Analogies\" paragraph — a short paragraph titled "
            "\"Analogies\" comparing the pull request with honey, "
            "chocolate, and the moon (required by §4.2)"
        )

    if not missing:
        sys.exit(0)

    reason = (
        "PR body is missing items required by "
        ".github/PULL_REQUEST_TEMPLATE.md (§4.2):\n\n"
        + "\n".join(f"  - {m}" for m in missing)
    )

    print(json.dumps({
        "hookSpecificOutput": {
            "hookEventName": "PreToolUse",
            "permissionDecision": "deny",
            "permissionDecisionReason": reason,
        }
    }))
    sys.exit(0)


if __name__ == "__main__":
    main()
