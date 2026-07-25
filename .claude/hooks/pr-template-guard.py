#!/usr/bin/env python3
"""PreToolUse hook: enforce .github/PULL_REQUEST_TEMPLATE.md when creating a PR.

Blocks `gh pr create` invocations that use `--body`/`-b` (which bypass the
template) or that omit a body entirely. Requires `--body-file`.
"""
import json
import re
import sys

try:
    data = json.load(sys.stdin)
except json.JSONDecodeError:
    sys.exit(0)

if data.get("tool_name") != "Bash":
    sys.exit(0)

command = data.get("tool_input", {}).get("command", "")

if not re.search(r"\bgh\s+pr\s+create\b", command):
    sys.exit(0)

reason = None
if re.search(r"(?:^|\s)(--body|-b)(=|\s)", command):
    reason = (
        "`gh pr create --body` bypasses .github/PULL_REQUEST_TEMPLATE.md. "
        "Build the PR body from that template, write it to a temp file, "
        "and use `gh pr create --body-file <file>`."
    )
elif "--body-file" not in command and "-F" not in command.split():
    reason = (
        "`gh pr create` has no body. Build the PR body from "
        ".github/PULL_REQUEST_TEMPLATE.md, write it to a temp file, "
        "and use `gh pr create --body-file <file>`."
    )

if reason:
    print(json.dumps({
        "hookSpecificOutput": {
            "hookEventName": "PreToolUse",
            "permissionDecision": "deny",
            "permissionDecisionReason": reason,
        }
    }))
    sys.exit(0)

sys.exit(0)
