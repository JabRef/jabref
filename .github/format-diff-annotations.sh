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
  /^\+\+\+ b\// { file = escape_property(substr($0, 7)); next }
  /^@@ / {
    # @@ -oldStart,oldCount +newStart,newCount @@
    match($2, /^-([0-9]+)(,([0-9]+))?/, o)
    match($3, /^\+([0-9]+)(,([0-9]+))?/, n)
    oldStart = o[1]; oldCount = (o[3] == "" ? 1 : o[3])
    newStart = n[1]; newCount = (n[3] == "" ? 1 : n[3])
    if (oldCount > 0) {                       # there are old lines -> anchor on them
      start = oldStart
      end = oldStart + oldCount - 1
    } else {                                  # pure addition / new file -> anchor on new lines
      start = newStart
      end = newStart + newCount - 1
    }
    if (start < 1) start = 1
    if (end < start) end = start
    printf "::error file=%s,line=%s,endLine=%s,title=%s::%s lines %s-%s.\n", file, start, end, title, messagePrefix, start, end
  }
'

# Make the job fail despite the successful annotations above.
exit 1
