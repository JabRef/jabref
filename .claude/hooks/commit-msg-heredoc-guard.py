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
import shlex
import sys

# PowerShell here-string openers/closers. In bash these are always a mistake.
HERESTRING_MARKERS = ("@'", "'@", '@"', '"@')


def find_subcommand_index(tokens, git_index):
    """Return the index of the subcommand after `git`, skipping global
    options. Only `-C` and `-c` are treated as taking a separate-token
    value; other dash-prefixed tokens are assumed to be no-arg flags or
    use the `--foo=value` form."""
    j = git_index + 1
    n = len(tokens)
    while j < n:
        tok = tokens[j]
        if tok in ("-C", "-c"):
            j += 2
            continue
        if tok.startswith("-"):
            j += 1
            continue
        return j
    return None


VIOLATION_MESSAGE = (
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


def tokens_invoke_git_commit(tokens):
    """True if `tokens` invoke `git commit`, even when global options
    (e.g. `-c key=val`, `--no-pager`) appear between `git` and `commit`."""
    for i, tok in enumerate(tokens):
        if tok != "git":
            continue
        sub_idx = find_subcommand_index(tokens, i)
        if sub_idx is not None and tokens[sub_idx] == "commit":
            return True
    return False


def is_herestring_token(tok):
    """True if a shlex token is a PowerShell here-string remnant.

    shlex strips the bash-meaningless quotes from `@'...'@` / `@"..."@`,
    leaving the literal `@` that bracketed each quote — so the token both
    starts and ends with `@` around a multiline body (`@\\n...\\n@`). Bash
    string literals that merely *contain* `@'`/`@"` (an email, an
    @-mention) keep those characters mid-token and do not exhibit this
    open-and-close `@` framing, so they are not flagged."""
    return tok.startswith("@") and tok.endswith("@") and "\n" in tok


def find_violation(command):
    try:
        tokens = shlex.split(command, posix=True)
    except ValueError:
        # Unbalanced quoting (often a malformed here-string). shlex can't
        # tokenize it, so fall back to a raw scan that respects token
        # boundaries well enough to still fire the guard.
        if "git commit" not in command:
            return None
        if not any(marker in command for marker in HERESTRING_MARKERS):
            return None
        if not re.search(r"(?:^|\s)@['\"]", command):
            return None
        return VIOLATION_MESSAGE

    if not tokens_invoke_git_commit(tokens):
        return None
    if not any(is_herestring_token(t) for t in tokens):
        return None
    return VIOLATION_MESSAGE


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
