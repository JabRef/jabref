# --*- Makefile -*--

# Set the ARGS variable to pass argumnents to test classes: 'make
# ARGS="[year]_[auth]_[firstpage]". Default is no arguments. S.G.

ARGS =

JFLAGS = 

# Java files that contain main classes:

MAIN_CLASS_SOURCES = ${wildcard tools/*.java}

MAIN_CLASSES = ${MAIN_CLASS_SOURCES:%.java=%.class}

# Java files that contain other classes:

JFILES = src/main/java/org/jabref/logic/util/BracketedExpressionExpander.java

CFILES = ${JFILES:src/main/java/%.java=bin/%.class}

CLASS_FILES = ${MAIN_CLASSES} ${CFILES}

# As recommended in
# https://www.gnu.org/software/make/manual/html_node/Syntax-of-Functions.html#Syntax-of-Functions
# (S.G.):
empty:=
space:= ${empty} ${empty}

CLASSPATH := $(subst ${space},:,$(subst src/main/java/,bin/,$(sort $(dir ${JFILES})))\
$(sort $(dir ${MAIN_CLASSES}))\
bin\
$(shell find ${HOME}/.gradle -name '*.jar' | sort -u)\
)

CLASSDIR = bin

export CLASSPATH

.PHONY: all clean cleanAll distclean display

all: ${CLASS_FILES}

display:
	@echo ${CLASSPATH}
	@echo ${CLASS_FILES}

%.class: %.java ${CFILES}
	javac $<

bin/%.class: src/main/java/%.java
	javac -d ${CLASSDIR} $<

run: ${MAIN_CLASSES}
	for FILE in $^; do ( \
		CLASS=$$(basename $$FILE .class); \
		set -x; \
		java $${CLASS} ${ARGS} \
	) done

clean cleanAll distclean:
	rm -f ${CLSFILES}
