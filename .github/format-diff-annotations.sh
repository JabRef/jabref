#!/usr/bin/env bash
# Emits GitHub Actions error annotations (with line ranges) for every hunk in the
# current `git diff`, then exits non-zero if the working directory is dirty.
#
# Used by the CI jobs that rewrite files (formatter, OpenRewrite) and then check
# that the working tree is clean. Each remaining hunk becomes an
# `::error file=...,line=...,endLine=...` annotation pointing at the affected lines.
#
# The annotation title and message are configurable so the same script can report
# formatting issues or OpenRewrite changes:
#   ANNOTATION_TITLE           annotation title          (default: "Formatting")
#   ANNOTATION_MESSAGE_PREFIX  message before the range  (default: formatter hint)
# The text " lines <start>-<end>." is appended to the message prefix.
#
# Usage: run from the directory that contains the git working tree to check.
set -euo pipefail

export ANNOTATION_TITLE="${ANNOTATION_TITLE:-Formatting}"
export ANNOTATION_MESSAGE_PREFIX="${ANNOTATION_MESSAGE_PREFIX:-Wrongly formatted. Run the formatter (see CONTRIBUTING.md) to fix}"

diff="$(git diff)"

if [ -z "$diff" ]; then
  echo "Working tree clean"
  exit 0
fi

printf '%s\n' "$diff" | gawk '
  # Mirror org.jabref.logic.util.GitHubActionsEscape#data: escape %, CR, LF.
  function escape_data(value) {
    gsub(/%/,  "%25", value)
    gsub(/\r/, "%0D", value)
    gsub(/\n/, "%0A", value)
    return value
  }
  # Mirror org.jabref.logic.util.GitHubActionsEscape#property: data escaping plus
  # : and , (critical for Windows paths like C:\foo\bar.bib).
  function escape_property(value) {
    value = escape_data(value)
    gsub(/:/, "%3A", value)
    gsub(/,/, "%2C", value)
    return value
  }
  BEGIN {
    title = escape_property(ENVIRON["ANNOTATION_TITLE"])
    messagePrefix = escape_data(ENVIRON["ANNOTATION_MESSAGE_PREFIX"])
  }
  # Emit one annotation per contiguous run of changed lines, anchored on the
  # old-side line numbers. The old side is the committed file the developer
  # has to fix, so those are the line numbers worth pointing at. Context lines
  # padded around each hunk are skipped, so the range matches the actual edit
  # rather than the whole `@@` hunk.
  function flush(  s, e) {
    if (!inRun) return
    s = runStart; e = runEnd
    if (e < s) e = s
    if (s < 1) s = 1
    printf "::error file=%s,line=%s,endLine=%s,title=%s::%s lines %s-%s.\n", file, s, e, title, messagePrefix, s, e
    inRun = 0
  }
  /^diff --git / { flush(); inHunk = 0; next }
  /^--- /        { next }          # old-file header
  /^index /      { next }
  /^\+\+\+ b\//  { flush(); inHunk = 0; file = escape_property(substr($0, 7)); next }
  /^@@ / {
    # @@ -oldStart,oldCount +newStart,newCount @@
    flush()
    match($2, /^-([0-9]+)/, o)
    oldLine = o[1]
    inHunk = 1
    next
  }
  {
    if (!inHunk) next
    c = substr($0, 1, 1)
    if (c == "-") {                           # removed/changed committed line
      if (!inRun) { inRun = 1; runStart = oldLine }
      runEnd = oldLine
      oldLine++
    } else if (c == "+") {                    # added line: does not consume an old line
      if (!inRun) { inRun = 1; runStart = oldLine; runEnd = oldLine }
    } else if (c == "\\") {                   # "\ No newline at end of file"
      # ignore
    } else {                                  # context line -> ends the current run
      flush()
      oldLine++
    }
  }
  END { flush() }
'

# Make the job fail despite the successful annotations above.
exit 1
