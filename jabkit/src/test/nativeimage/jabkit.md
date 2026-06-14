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
$ "$JABKIT" preferences export build/tmp/jabkit-prefs-smoke.xml 2>/dev/null; echo $?
0
$ "$JABKIT" pseudonymize --porcelain --force --input=src/test/resources/org/jabref/toolkit/commands/origin.bib --output=build/tmp/origin.pseudo.bib --key=build/tmp/origin.pseudo.csv > build/tmp/pseudonymize.out 2>/dev/null; echo $?
0
$ grep -c "Pseudonymizing library" build/tmp/pseudonymize.out
1
$ "$JABKIT" convert --porcelain --input=src/test/resources/org/jabref/toolkit/commands/origin.bib --input-format=bibtex --output=build/tmp/convert.bib 2>/dev/null; echo $?
0
$ grep -c "@Book{" build/tmp/convert.bib
3
$ "$JABKIT" citationkeys generate --porcelain --input=src/test/resources/org/jabref/toolkit/commands/origin.bib 2>/dev/null | grep -c "@Book{"
3
$ "$JABKIT" doi-to-bibtex --porcelain 10.1109/ICWS.2007.59 > build/tmp/doi-to-bibtex.out 2>/dev/null; echo $?
0
$ grep -i -c "10.1109/icws.2007.59" build/tmp/doi-to-bibtex.out
1
$ grep -c "BPEL4Chor" build/tmp/doi-to-bibtex.out
1
$ "$JABKIT" fetch --porcelain --provider=CrossRef --query="JabRef BibTeX-based literature management software" > build/tmp/fetch-crossref.out 2>/dev/null; echo $?
0
$ grep -i -c "10.47397/tb/44-3/tb138kopp-jabref" build/tmp/fetch-crossref.out
1
