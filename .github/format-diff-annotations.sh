#!/usr/bin/env bash
# Emits GitHub Actions error annotations (with line ranges) for every hunk in the
# current `git diff`, then exits non-zero if the working directory is dirty.
#
# Used by the formatting jobs: after a formatter rewrites files, the remaining diff
# marks the lines that were committed in the wrong format. Each hunk becomes an
# `::error file=...,line=...,endLine=...` annotation pointing at the original lines.
#
# Usage: run from the directory that contains the git working tree to check.
set -euo pipefail

diff="$(git diff)"

if [ -z "$diff" ]; then
  echo "Formatting OK"
  exit 0
fi

printf '%s\n' "$diff" | gawk '
  # Mirror org.jabref.logic.util.GitHubActionsEscape#property: escape %, CR, LF,
  # then additionally : and , (critical for Windows paths like C:\foo\bar.bib).
  function escape_property(value) {
    gsub(/%/,  "%25", value)
    gsub(/\r/, "%0D", value)
    gsub(/\n/, "%0A", value)
    gsub(/:/,  "%3A", value)
    gsub(/,/,  "%2C", value)
    return value
  }
  /^\+\+\+ b\// { file = escape_property(substr($0, 7)); next }
  /^@@ / {
    # @@ -oldStart,oldCount +newStart,newCount @@
    match($2, /^-([0-9]+)(,([0-9]+))?/, m)
    start = m[1]
    count = (m[3] == "" ? 1 : m[3])
    if (count == 0) { end = start }          # pure addition -> anchor on single line
    else { end = start + count - 1 }
    printf "::error file=%s,line=%s,endLine=%s,title=Formatting::Wrongly formatted. Run the formatter (see CONTRIBUTING.md) to fix lines %s-%s.\n", file, start, end, start, end
  }
'

# Make the job fail despite the successful annotations above.
exit 1
