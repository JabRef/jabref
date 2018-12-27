#!/bin/bash
# taken from https://github.com/lhotari/travis-gradle-test-failures-to-console/blob/master/travis/junit-errors-to-stdout.sh
IFS='
'
DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
if [ "$TRAVIS" = "true" ]; then
	#echo 'Installing xml-twig-tools and xsltproc....'
	sudo apt-get install -qq -y --force-yes xml-twig-tools xsltproc > /dev/null
fi
ROOTDIR="$1"
if [ -z "$ROOTDIR" ]; then
	ROOTDIR="."
fi
echo 'Formatting results...'
FILES=$(find "$ROOTDIR" -path '*/build/test-results/*.xml' | xargs --no-run-if-empty xml_grep --files --cond 'testsuite[@failures > 0 or @errors > 0]')
if [ -n "$FILES" ]; then
	for file in $FILES; do
		echo "Formatting $file"
		if [ -f "$file" ]; then
			echo '====================================================='
			xsltproc "$DIR/junit-xml-format-errors.xsl" "$file"
		fi
	done
	echo '====================================================='
else
	echo 'No */build/test-results/*.xml files found with failing tests.'
fi
