# --*- Makefile -*-------------------------------------------------------------

# Compile Java main classes in the tools directory, and the classes in
# the files liste din the JFILES variable.
#
# Run tests in a test directory (tests/ by default) and report if
# all tests pass.
#
# USAGE:
#     make clean
#     make distclean
#     make tests
#     make

#------------------------------------------------------------------------------
# Include local configuration files from this directory:

MAKECONF_FILES = ${filter-out %~, ${wildcard Makeconf*}}

ifneq ("${MAKECONF_FILES}","")
include ${MAKECONF_FILES}
endif

#------------------------------------------------------------------------------
# ARGS holds arguments to be passed to the run main Java class; empty
# by default but can be provided on the comamnd line:

# make run ARGS="tools/tests/inputs/Grazulis2009.bib [year]_[auth:lower]_[pages].pdf"

ARGS =

JFLAGS = 

# Java files that contain main classes:

MAIN_CLASS_SOURCES = ${wildcard tools/*.java}

MAIN_CLASSES = ${MAIN_CLASS_SOURCES:%.java=%.class}

# Java files that contain other classes:

JFILES = src/main/java/org/jabref/logic/util/BracketedExpressionExpander.java

CFILES = ${JFILES:src/main/java/%.java=bin/%.class}

CLASS_FILES = ${MAIN_CLASSES} ${CFILES}

#------------------------------------------------------------------------------
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

#------------------------------------------------------------------------------
# File and directory settings for test cases:

BIN_DIR  = tools

TEST_DIR = tools/tests/cases
OUTP_DIR = tools/tests/outputs

INP_FILES  = ${wildcard ${TEST_DIR}/*.inp}
OPT_FILES  = ${wildcard ${TEST_DIR}/*.opt}
SH_FILES   = ${wildcard ${TEST_DIR}/*.sh}

INP_DIFFS = ${INP_FILES:${TEST_DIR}/%.inp=${OUTP_DIR}/%.diff}
INP_OUTS  = ${INP_FILES:${TEST_DIR}/%.inp=${OUTP_DIR}/%.out}

OPT_DIFFS = ${OPT_FILES:${TEST_DIR}/%.opt=${OUTP_DIR}/%.diff}
OPT_OUTS  = ${OPT_FILES:${TEST_DIR}/%.opt=${OUTP_DIR}/%.out}

SH_DIFFS = ${SH_FILES:${TEST_DIR}/%.sh=${OUTP_DIR}/%.diff}
SH_OUTS  = ${SH_FILES:${TEST_DIR}/%.sh=${OUTP_DIR}/%.out}

DIFF_FILES = $(sort ${INP_DIFFS} ${OPT_DIFFS} ${SH_DIFFS})
OUTP_FILES = $(sort ${INP_OUTS} ${OPT_OUTS} ${SH_OUTS})

#------------------------------------------------------------------------------

.PHONY: all clean cleanAll distclean display

all: ${CLASS_FILES}

display:
	@echo ${CLASSPATH}
	@echo ${CLASS_FILES}

#------------------------------------------------------------------------------
# Include Makefiles with additional rules for this directory:

MAKELOCAL_FILES = ${filter-out %~, ${wildcard Makelocal*}}

ifneq ("${MAKELOCAL_FILES}","")
include ${MAKELOCAL_FILES}
endif

#------------------------------------------------------------------------------

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

#------------------------------------------------------------------------------

test tests: ${DIFF_FILES}

out outputs: ${OUTP_FILES}

${DIFF_FILES}: ${MAIN_CLASSES}

#------------------------------------------------------------------------------
# Rules to run script-specific tests:

${OUTP_DIR}/%.diff: ${TEST_DIR}/%.inp ${TEST_DIR}/%.opt ${OUTP_DIR}/%.out
	-@printf "%-50s " "$<:" ; \
	if [ ! -e ${TEST_DIR}/$*.chk ] || ${TEST_DIR}/$*.chk; then \
		java $(shell echo $* | sed -e 's/_[0-9]*$$//') \
		    $(shell grep -v '^#' ${word 2, $^}) \
	    	$< 2>&1 \
		| diff ${OUTP_DIR}/$*.out - > $@ ; \
		if [ $$? = 0 ]; then echo "OK"; else echo "FAILED:"; cat $@; fi; \
	else \
		touch $@; \
	fi

${OUTP_DIR}/%.diff: ${TEST_DIR}/%.inp ${OUTP_DIR}/%.out
	-@printf "%-50s " "$<:" ; \
	if [ ! -e ${TEST_DIR}/$*.chk ] || ${TEST_DIR}/$*.chk; then \
		java $(shell echo $* | sed -e 's/_[0-9]*$$//') \
		    $< 2>&1 \
		| diff ${OUTP_DIR}/$*.out - > $@ ; \
		if [ $$? = 0 ]; then echo "OK"; else echo "FAILED:"; cat $@; fi; \
	else \
		touch $@; \
	fi

${OUTP_DIR}/%.diff: ${TEST_DIR}/%.opt ${OUTP_DIR}/%.out
	-@printf "%-50s " "$<:" ; \
	if [ ! -e ${TEST_DIR}/$*.chk ] || ${TEST_DIR}/$*.chk; then \
		java $(shell echo $* | sed -e 's/_[0-9]*$$//') \
		    $(shell grep -v '^#' $<) \
		2>&1 \
		| diff ${OUTP_DIR}/$*.out - > $@ ; \
		if [ $$? = 0 ]; then echo "OK"; else echo "FAILED:"; cat $@; fi; \
	else \
		touch $@; \
	fi

${OUTP_DIR}/%.diff: ${TEST_DIR}/%.sh ${OUTP_DIR}/%.out
	-@printf "%-50s " "$<:" ; \
	if [ ! -e ${TEST_DIR}/$*.chk ] || ${TEST_DIR}/$*.chk; then \
		$< 2>&1 | diff ${OUTP_DIR}/$*.out - > $@ ; \
		if [ $$? = 0 ]; then echo "OK"; else echo "FAILED:"; cat $@; fi; \
	else \
		touch $@; \
	fi

# Rules to generate sample test outputs:

${OUTP_DIR}/%.out: ${TEST_DIR}/%.inp ${TEST_DIR}/%.opt
	-@test -f $@ || echo "$@:"
	-@test -f $@ || \
	java $(shell echo $* | sed -e 's/_[0-9]*$$//') \
		$(shell grep -v '^#' ${word 2, $^}) \
		$< \
	2>&1 \
	| tee $@
	-@touch $@

${OUTP_DIR}/%.out: ${TEST_DIR}/%.inp
	-@test -f $@ || echo "$@:"
	-@test -f $@ || \
	java $(shell echo $* | sed -e 's/_[0-9]*$$//') \
		$< \
	2>&1 \
	| tee $@
	-@touch $@

${OUTP_DIR}/%.out: ${TEST_DIR}/%.opt
	-@test -f $@ || echo "$@:"
	-@test -f $@ || \
	java $(shell echo $* | sed -e 's/_[0-9]*$$//') \
		$(shell grep -v '^#' $<) \
	2>&1 \
	| tee $@
	-@touch $@

${OUTP_DIR}/%.out: ${TEST_DIR}/%.inp
	-@test -f $@ || echo "$@:"
	-@test -f $@ || \
	java $(shell echo $* | sed -e 's/_[0-9]*$$//') \
		$< \
	2>&1 \
	| tee $@
	-@touch $@

${OUTP_DIR}/%.out: ${TEST_DIR}/%.sh
	-@test -f $@ || echo "$@:"
	-@test -f $@ || \
		$< 2>&1 | tee $@
	-@touch $@

#------------------------------------------------------------------------------

.PHONY: failed listdiff

failed listdiff: ## test
	@-find ${OUTP_DIR} -type f -name '*.diff' -size +0 | sort -u

#------------------------------------------------------------------------------

clean:
	rm -f ${DIFF_FILES}

distclean cleanAll: clean
	rm -f ${CLASS_FILES}
