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
