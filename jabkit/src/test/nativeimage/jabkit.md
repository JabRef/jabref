$ JABKIT=build/native/nativeCompile/jabkit
$ "$JABKIT" --help 2>/dev/null | grep "^Usage:"
Usage: jabkit [-dhpv] [COMMAND]
$ "$JABKIT" check consistency --porcelain --input=src/test/resources/org/jabref/toolkit/commands/origin.bib 2>/dev/null; echo $?
0
$ mkdir -p build/tmp
$ "$JABKIT" check integrity --porcelain --input=src/test/resources/org/jabref/toolkit/commands/origin.bib > build/tmp/integrity.out 2>/dev/null; echo $?
1
$ grep -c "capital letters are not masked using curly brackets" build/tmp/integrity.out
1
$ "$JABKIT" convert --porcelain --input=src/test/resources/org/jabref/toolkit/commands/origin.bib --input-format=bibtex --output=build/tmp/convert.bib 2>/dev/null; echo $?
0
$ grep -c "@Book{" build/tmp/convert.bib
3
$ "$JABKIT" citationkeys generate --porcelain --input=src/test/resources/org/jabref/toolkit/commands/origin.bib 2>/dev/null | grep -c "@Book{"
3
