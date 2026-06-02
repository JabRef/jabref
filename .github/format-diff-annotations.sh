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
  # Emit one annotation per contiguous run of changed lines. A run that touches
  # any old-side line (a removal, or a removal+addition reformat) is anchored on
  # the old-side range: the committed line the developer has to fix. A run with
  # only additions (`@@ -X,0 +Y,N @@` insertions) has no old-side line to point
  # at, so it is anchored on the new-side range instead. Context lines padded
  # around each hunk are skipped, so the range matches the actual edit rather
  # than the whole `@@` hunk.
  function flush(  s, e) {
    if (!inRun) return
    if (hasOld) { s = oldStartLine; e = oldEndLine }
    else        { s = newStartLine; e = newEndLine }
    if (e < s) e = s
    if (s < 1) s = 1
    printf "::error file=%s,line=%s,endLine=%s,title=%s::%s lines %s-%s.\n", file, s, e, title, messagePrefix, s, e
    inRun = 0; hasOld = 0; hasNew = 0
  }
  /^diff --git / { flush(); inHunk = 0; next }
  # Old-file header. For deletions the new-file header is "+++ /dev/null", so the
  # old side ("--- a/...") is the only place the path appears. Annotations are
  # anchored on old-side line numbers anyway, so this is the right path to point at.
  /^--- a\//     { flush(); inHunk = 0; file = escape_property(substr($0, 7)); next }
  /^--- /        { next }          # "--- /dev/null" (new file); path comes from "+++ b/"
  /^index /      { next }
  /^\+\+\+ b\//  { flush(); inHunk = 0; file = escape_property(substr($0, 7)); next }
  /^@@ / {
    # @@ -oldStart,oldCount +newStart,newCount @@
    flush()
    match($2, /^-([0-9]+)/, o)
    match($3, /^\+([0-9]+)/, n)
    oldLine = o[1]
    newLine = n[1]
    inHunk = 1
    next
  }
  {
    if (!inHunk) next
    c = substr($0, 1, 1)
    if (c == "-") {                           # removed/changed committed line
      inRun = 1
      if (!hasOld) { hasOld = 1; oldStartLine = oldLine }
      oldEndLine = oldLine
      oldLine++
    } else if (c == "+") {                    # added line: consumes a new-side line only
      inRun = 1
      if (!hasNew) { hasNew = 1; newStartLine = newLine }
      newEndLine = newLine
      newLine++
    } else if (c == "\\") {                   # "\ No newline at end of file"
      # ignore
    } else {                                  # context line -> ends the current run
      flush()
      oldLine++
      newLine++
    }
  }
  END { flush() }
'

# Make the job fail despite the successful annotations above.
exit 1
