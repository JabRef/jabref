#!/usr/bin/env python3
"""PreToolUse hook: block PowerShell here-strings in Bash-tool git commits.

The Bash tool runs under bash, but the agent's default environment shell is
PowerShell. The PowerShell here-string syntax `@'...'@` (and `@"..."@`) is a
parse no-op in bash: the leading `@` and trailing `@` are treated as literal
characters glued onto the quoted body. A command like

    git commit -m @'
    Reset submodules to match main
    '@

therefore produces a commit whose subject is a lone `@`, with the real text
pushed into the body. This has happened repeatedly. We deny it here so the
agent rewrites the command using bash-native quoting instead, e.g.

    git commit -m "Reset submodules to match main" \
               -m "Co-Authored-By: ..."

or a real bash here-doc passed via -F:

    git commit -F - <<'EOF'
    Subject line
    EOF

This guard only fires for the Bash tool, so PowerShell-tool commits (where the
here-string IS valid) are unaffected.
"""
import json
import re
import sys

# PowerShell here-string openers/closers. In bash these are always a mistake.
HERESTRING_MARKERS = ("@'", "'@", '@"', '"@')


def find_violation(command):
    if "git commit" not in command:
        return None
    if not any(marker in command for marker in HERESTRING_MARKERS):
        return None
    # Require an opener at a token boundary to avoid flagging stray `'@`
    # inside, e.g., an email address. Openers follow whitespace.
    if not re.search(r"(?:^|\s)@['\"]", command):
        return None
    return (
        "PowerShell here-string syntax (`@'...'@` / `@\"...\"@`) detected in a "
        "Bash-tool `git commit`. The Bash tool runs bash, where the leading "
        "`@` and trailing `@` are literal characters — this yields a commit "
        "whose subject is a lone `@`. Use bash-native quoting instead:\n"
        "  git commit -m \"Subject line\" -m \"Body / Co-Authored-By: ...\"\n"
        "or a real here-doc:\n"
        "  git commit -F - <<'EOF'\n"
        "  Subject line\n"
        "\n"
        "  Body\n"
        "  EOF"
    )


def main():
    try:
        data = json.load(sys.stdin)
    except json.JSONDecodeError:
        sys.exit(0)

    if data.get("tool_name") != "Bash":
        sys.exit(0)

    command = data.get("tool_input", {}).get("command", "")

    reason = find_violation(command)
    if reason:
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
