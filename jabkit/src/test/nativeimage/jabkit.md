$ JABKIT=build/native/nativeCompile/jabkit
$ "$JABKIT" --help 2>/dev/null | grep "^Usage:"
Usage: jabkit [-dhpv] [COMMAND]
$ "$JABKIT" check consistency --input=src/test/resources/org/jabref/toolkit/commands/origin.bib 2>/dev/null | grep -E "^(No errors found\.|Consistency check completed)"
No errors found.
Consistency check completed
$ "$JABKIT" check integrity --input=src/test/resources/org/jabref/toolkit/commands/origin.bib 2>/dev/null | grep -c "capital letters are not masked using curly brackets"
1
