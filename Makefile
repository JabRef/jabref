# --*- Makefile -*--

JFLAGS = 

JFILES   = ${wildcard tools/*.java}
CLSFILES = ${JFILES:%.java=%.class}

.PHONY: all clean cleanAll distclean

CLASSPATH := $(join :,$(sort $(dir ${JFILES})))

export CLASSPATH

all: ${CLSFILES}

%.class: %.java
	javac $<

run: ${CLSFILES}
	for FILE in $^; do ( \
		CLASS=$$(basename $$FILE .class); \
		set -x; \
		java $${CLASS} ${SMILES} \
	) done

clean cleanAll distclean:
	rm -f ${CLSFILES}
