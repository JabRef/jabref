$ JABKIT=build/native/nativeCompile/jabkit
$ "$JABKIT" --help 2>/dev/null | grep "^Usage:"
Usage: jabkit [-dhpv] [COMMAND]
$ "$JABKIT" check consistency --input=src/test/resources/org/jabref/toolkit/commands/origin.bib 2>/dev/null | grep -E "^Consistency check completed"
Consistency check completed
$ mkdir -p build/tmp
$ "$JABKIT" check integrity --input=src/test/resources/org/jabref/toolkit/commands/origin.bib > build/tmp/integrity.out 2>/dev/null; echo $?
1
$ grep -c "capital letters are not masked using curly brackets" build/tmp/integrity.out
1
$ "$JABKIT" convert --input=src/test/resources/org/jabref/toolkit/commands/origin.bib --input-format=bibtex 2>/dev/null | grep "to 'bibtex'"
Converting 'src/test/resources/org/jabref/toolkit/commands/origin.bib' to 'bibtex'.
$ "$JABKIT" citationkeys generate --input=src/test/resources/org/jabref/toolkit/commands/origin.bib 2>/dev/null | grep "Regenerating citation keys"
Regenerating citation keys according to metadata.
