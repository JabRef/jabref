#!/usr/bin/python

# Python script for combining several journal abbreviation lists
# and producing an alphabetically sorted list. If the same journal
# names are repeated, only the version found last is retained.
#
# Usage: combineJournalLists.py outfile infile1 infile2 ...

import sys
import fnmatch
import os

outFile = sys.argv[1]
dictionary = dict()
for i in range(2,len(sys.argv)):
    count = 0
    f = open(sys.argv[i], "r")
    for line in f:
	if line[0] != "#":
	    count = count+1
	    parts = line.partition("=")
	    dictionary[parts[0].strip()] = line.strip()
    f.close()
    print sys.argv[i]+": "+str(count)

print "Combined key count: "+str(len(dictionary))

f = open(outFile, "w")
for key in sorted(dictionary.iterkeys()):
      f.write(dictionary[key]+"\n")
f.close()
