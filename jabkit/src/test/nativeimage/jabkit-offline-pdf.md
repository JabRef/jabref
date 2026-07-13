$ JABKIT=build/native/nativeCompile/jabkit
$ mkdir -p build/tmp
$ cp ../jablib/src/test/resources/org/jabref/logic/importer/util/LNCS-minimal.pdf build/tmp/native-metadata.pdf
$ sed "s|__PDF_PATH__|$PWD/build/tmp/native-metadata.pdf|" src/test/nativeimage/native-metadata-template.bib > build/tmp/native-metadata.bib
$ "$JABKIT" pdf update --porcelain --format=bibtex-attachment --citation-key=NativePdfSmoke --input-format=bibtex --input=build/tmp/native-metadata.bib > build/tmp/pdfupdate.out 2>/dev/null; echo $?
0
$ grep -c "Successfully embedded metadata" build/tmp/pdfupdate.out
1
