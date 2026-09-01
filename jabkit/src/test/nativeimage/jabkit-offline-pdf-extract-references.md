$ JABKIT=build/native/nativeCompile/jabkit
$ mkdir -p build/tmp
$ cp src/test/resources/pdfs/ieee-paper.pdf build/tmp/native-extract-references.pdf
$ "$JABKIT" pdf extract-references --porcelain --mode=RULE_BASED build/tmp/native-extract-references.pdf > build/tmp/extract-references.out 2>/dev/null; echo $?
0
$ grep -c "@Article\|@InProceedings" build/tmp/extract-references.out
5
