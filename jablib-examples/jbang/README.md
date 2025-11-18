# `jablib` examples

This directory contains [`JBang`](https://www.jbang.dev/) examples to show how to use some `jablib` features.

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
