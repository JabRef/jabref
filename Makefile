# --*- Makefile -*--

JFLAGS = 

MAIN_CLASS_SOURCES = ${wildcard tools/*.java}

MAIN_CLASSES = ${MAIN_CLASS_SOURCES:%.java=%.class}

JFILES = src/main/java/org/jabref/logic/util/BracketedExpressionExpander.java

CLSFILES = ${JFILES:src/main/java/%.java=bin/%.class} ${MAIN_CLASSES}

# As recommended in
# https://www.gnu.org/software/make/manual/html_node/Syntax-of-Functions.html#Syntax-of-Functions
# (S.G.):
empty:=
space:= ${empty} ${empty}

CLASSPATH := $(subst ${space},:,$(subst src/main/java/,bin/,$(sort $(dir ${JFILES})))\
$(sort $(dir ${MAIN_CLASSES}))\
)bin

CLASSDIR = bin

export CLASSPATH


.PHONY: all clean cleanAll distclean display

all: ${CLSFILES}

display:
	@echo ${CLASSPATH}
	@echo ${CLSFILES}

%.class: %.java
	javac $<

bin/%.class: src/main/java/%.java
	javac -d ${CLASSDIR} $<

run: ${MAIN_CLASSES}
	for FILE in $^; do ( \
		CLASS=$$(basename $$FILE .class); \
		set -x; \
		java $${CLASS} ${SMILES} \
	) done

clean cleanAll distclean:
	rm -f ${CLSFILES}
