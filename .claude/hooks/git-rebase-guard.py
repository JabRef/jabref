#!/usr/bin/env python3
"""PreToolUse hook: forbid `git rebase` and force-push variants.

JabRef policy (see AGENTS.md "Git & PR Etiquette"): sync branches with
`git fetch upstream --prune && git merge upstream/main`. Rebasing rewrites
commit SHAs already pushed, breaks review threads pinned to commits, and
forces a `--force-push` that this project disallows.

The check tokenizes the command with shlex so matches inside quoted
strings or heredoc bodies (e.g. a commit message containing the words
"git rebase") are not flagged — only actual `git rebase` invocations.
"""
import json
import shlex
import sys


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


def find_violation(tokens):
    for i, tok in enumerate(tokens):
        if tok != "git":
            continue
        sub_idx = find_subcommand_index(tokens, i)
        if sub_idx is None:
            continue
        sub = tokens[sub_idx]
        rest = tokens[sub_idx + 1:]

        if sub == "rebase":
            return (
                "`git rebase` is forbidden by JabRef policy (AGENTS.md > "
                "Git & PR Etiquette). Sync with upstream using:\n"
                "  git fetch upstream --prune\n"
                "  git merge upstream/main\n"
                "Resolve conflicts inside the merge commit."
            )

        if sub == "pull" and any(t == "-r" or t.startswith("--rebase") for t in rest):
            return (
                "`git pull --rebase` is forbidden by JabRef policy "
                "(AGENTS.md > Git & PR Etiquette). Use:\n"
                "  git fetch upstream --prune\n"
                "  git merge upstream/main"
            )

        if sub == "push":
            force_flags = {"--force", "--force-with-lease",
                           "--force-if-includes", "-f"}
            forced = any(
                t in force_flags
                or t.startswith("--force-with-lease=")
                or t.startswith("--force-if-includes=")
                or t.startswith("+")  # +-prefixed refspec = force update
                for t in rest
            )
            if forced:
                return (
                    "Force-push is forbidden by JabRef policy (AGENTS.md > "
                    "Git & PR Etiquette). This includes `--force`, "
                    "`--force-with-lease`, `--force-if-includes`, `-f`, and "
                    "`+`-prefixed refspecs (e.g. `+HEAD:main`). If branch "
                    "history diverged because of an attempted rebase, "
                    "recover via merge instead — do not overwrite the remote."
                )

    return None


def main():
    try:
        data = json.load(sys.stdin)
    except json.JSONDecodeError:
        sys.exit(0)

    if data.get("tool_name") != "Bash":
        sys.exit(0)

    command = data.get("tool_input", {}).get("command", "")

    try:
        tokens = shlex.split(command, posix=True)
    except ValueError:
        sys.exit(0)

    reason = find_violation(tokens)
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
