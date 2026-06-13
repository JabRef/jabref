#!/usr/bin/env python3
"""PreToolUse hook: ensure CHECKLIST.md is actually done before a PR.

Blocks `gh pr create` / `gh pr edit` when the PR body does not prove that
every item in CHECKLIST.md was worked through. The PR template asks the AI
to paste the completed CHECKLIST.md into the "AI usage" section; this hook
verifies that:

  1. Every checklist item from CHECKLIST.md is present in the PR body.
  2. No item is left as a placeholder `[.]` or an unfinished `[ ]` (TODO).
     Each must be marked `[x]` (done) or `[/]` (not applicable).
"""
import json
import re
import shlex
import sys
from pathlib import Path

CHECKLIST = Path("CHECKLIST.md")

# Matches "- [x] text", "- [ ] text", "- [.] text", "- [/] text".
ITEM_RE = re.compile(r"^\s*-\s*\[(.)\]\s*(.+?)\s*$")


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


def norm(text):
    return re.sub(r"\s+", " ", text).strip().lower()


def checklist_items():
    """Return list of (normalized_text, raw_text) for every CHECKLIST.md item."""
    items = []
    for line in CHECKLIST.read_text(encoding="utf-8", errors="replace").splitlines():
        m = ITEM_RE.match(line)
        if m:
            items.append((norm(m.group(2)), m.group(2)))
    return items


def body_marks(body):
    """Return list of (normalized_text, mark) for every checkbox line in the body."""
    marks = []
    for line in body.splitlines():
        m = ITEM_RE.match(line)
        if m:
            marks.append((norm(m.group(2)), m.group(1)))
    return marks


def lookup_mark(marks, key):
    """Find the mark for a checklist item.

    An item matches a body line when the line's text equals the item text or
    begins with it. The prefix form lets contributors append a trailing note
    (e.g. "... passes. (no Java changed)") while still satisfying the item.
    Exact matches win over prefix matches.
    """
    for text, mark in marks:
        if text == key:
            return mark
    for text, mark in marks:
        if text.startswith(key):
            return mark
    return None


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

    if not CHECKLIST.is_file():
        sys.exit(0)

    try:
        tokens = shlex.split(cmd, posix=True)
    except ValueError:
        sys.exit(0)

    body = extract_body(tokens)
    if body is None:
        # pr-template-guard.py handles the "no body" case; do not duplicate.
        sys.exit(0)

    items = checklist_items()
    if not items:
        sys.exit(0)

    marks = body_marks(body)

    missing = []   # item text not present in body at all
    unfinished = []  # item present but still [.] or [ ]

    for key, raw in items:
        mark = lookup_mark(marks, key)
        if mark is None:
            missing.append(raw)
        elif mark not in ("x", "/", "X"):
            unfinished.append(raw)

    if not missing and not unfinished:
        sys.exit(0)

    parts = [
        "CHECKLIST.md is not done. Go through CHECKLIST.md one item at a "
        "time, fix the code/PR until each holds, and paste the completed "
        "checklist into the PR body (mark every item [x] done or [/] not "
        "applicable; no [.] placeholders or [ ] TODOs)."
    ]
    if missing:
        parts.append(
            "\nMissing from PR body:\n"
            + "\n".join(f"  - {m}" for m in missing)
        )
    if unfinished:
        parts.append(
            "\nStill unfinished (left as [.] or [ ]):\n"
            + "\n".join(f"  - {u}" for u in unfinished)
        )

    print(json.dumps({
        "hookSpecificOutput": {
            "hookEventName": "PreToolUse",
            "permissionDecision": "deny",
            "permissionDecisionReason": "\n".join(parts),
        }
    }))
    sys.exit(0)


if __name__ == "__main__":
    main()
