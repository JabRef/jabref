$ JABKIT=build/native/nativeCompile/jabkit
$ mkdir -p build/tmp
$ "$JABKIT" doi-to-bibtex --porcelain 10.1109/ICWS.2007.59 > build/tmp/doi-to-bibtex.out 2>/dev/null; echo $?
0
$ grep -qi "10.1109/icws.2007.59" build/tmp/doi-to-bibtex.out; echo $?
0
$ "$JABKIT" fetch --porcelain --provider=CrossRef --query="JabRef BibTeX-based literature management software" > build/tmp/fetch-crossref.out 2>/dev/null; echo $?
0
$ grep -qi "10.47397/tb/44-3/tb138kopp-jabref" build/tmp/fetch-crossref.out; echo $?
0
$ "$JABKIT" get-cited-works --porcelain 10.1016/j.jksuci.2024.102118 > build/tmp/get-cited-works.out 2>/dev/null; echo $?
0
$ grep -q "^@" build/tmp/get-cited-works.out; echo $?
0
$ "$JABKIT" get-citing-works --porcelain 10.1016/j.jksuci.2024.102118 > build/tmp/get-citing-works.out 2>/dev/null; echo $?
0
$ grep -q "^@" build/tmp/get-citing-works.out; echo $?
0
