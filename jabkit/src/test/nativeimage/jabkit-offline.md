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
$ "$JABKIT" generate-bib-from-aux --porcelain --aux=src/test/resources/org/jabref/toolkit/commands/paper.aux --input=src/test/resources/org/jabref/toolkit/commands/origin.bib --output=build/tmp/generated.bib; echo $?
0
$ grep -c "Darwin1888" build/tmp/generated.bib
1
$ grep -c "Einstein1920" build/tmp/generated.bib
1
$ "$JABKIT" search --porcelain --query='author =~ Einstein' --input=src/test/resources/org/jabref/toolkit/commands/origin.bib > build/tmp/search.out 2>/dev/null; echo $?
0
$ grep -c "^@Book{Einstein1920," build/tmp/search.out
1
$ grep -c "^@Book{" build/tmp/search.out
1
$ cp ../jablib/src/test/resources/org/jabref/logic/importer/util/LNCS-minimal.pdf build/tmp/native-metadata.pdf
$ sed "s|__PDF_PATH__|$PWD/build/tmp/native-metadata.pdf|" src/test/nativeimage/native-metadata-template.bib > build/tmp/native-metadata.bib
$ "$JABKIT" pdf update --porcelain --format=bibtex-attachment --citation-key=NativePdfSmoke --input-format=bibtex --input=build/tmp/native-metadata.bib > build/tmp/pdfupdate.out 2>/dev/null; echo $?
0
$ grep -c "Successfully embedded metadata" build/tmp/pdfupdate.out
1
