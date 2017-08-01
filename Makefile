# --*- Makefile -*--

JFLAGS = 

MAIN_CLASSES = ${wildcard tools/*.java}

JFILES   = ${wildcard tools/*.java} \
	src/main/java/org/jabref/logic/util/BracketedExpressionExpander.java

CLSFILES = ${JFILES:%.java=%.class}

# As recommended in
# https://www.gnu.org/software/make/manual/html_node/Syntax-of-Functions.html#Syntax-of-Functions
# (S.G.):
empty:=
space:= ${empty} ${empty}

CLASSPATH := $(subst ${space},:,$(subst src/main/java/,bin/,$(sort $(dir ${JFILES})))):bin

CLASSDIR = bin

export CLASSPATH


.PHONY: all clean cleanAll distclean display

all: ${CLSFILES}

display:
	@echo ${CLASSPATH}

%.class: %.java
	javac -d ${CLASSDIR} $<

run: ${MAIN_CLASSES}
	for FILE in $^; do ( \
		CLASS=$$(basename $$FILE .java); \
		set -x; \
		java $${CLASS} ${SMILES} \
	) done

clean cleanAll distclean:
	rm -f ${CLSFILES}
