# `jablib` examples

This directory contains [`JBang`](https://www.jbang.dev/) examples to show how to use some `jablib` features.

## Running the examples

If you have node installed:

```terminal
npx @jbangdev/jbang doi_to_bibtex.java
```

In case, you don't have node installed, you can [download gg.cmd](https://github.com/eirikb/gg/releases/latest/download/gg.cmd) and then run `sh -x gg.cmd jbang doi_to_bibtex.java` on Linux.
On Windows, it is:

```cmd
.\gg.cmd jbang doi_to_bibtex.java
```

One can also [download and install JBang](https://www.jbang.dev/download/) and then just run:

```terminal
jbang doi_to_bibtex.java
```

## Development

In case you need to modify `jablib`, you can do it while working on your script.

You need then to add each modified file following this pattern:

```java
//SOURCES ../jablib/src/main/java/org/jabref/logic/citation/repository/BibEntrySerializer.java
```

The list of modified files can be generated using git and some Linux command line tools as follows:

```bash
git diff --name-status --diff-filter=AMCR --find-renames=50% --find-copies=50% main | awk -F '\t' '($1=="A"||$1=="M"){print $2} ($1 ~ /^C/ || $1 ~ /^R/){print $3}' | grep 'jablib/src/main' | grep -v 'module-info' | grep -v '\.properties$' | sed 's#\(.*\)#//SOURCES ../\1#'
```
