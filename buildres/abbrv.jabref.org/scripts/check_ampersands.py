#!/usr/bin/env python3

"""
Python script for checking if all Ampersands in .csv journal abbreviation files are
unescaped. This convention is enforced to ensure that abbreviations of journal titles
can be done without error.

The script will raise a ValueError() in case escaped ampersands are found, and will
also provide the row and column in which they were found (1 -indexed). The script does
NOT automatically fix these errors. This should be done manually.

The script will automatically run whenever there is a push to the main branch of the
abbreviations repo (abbrv.jabref.org) using GitHub Actions.
"""

import os
import itertools

# Get all file names in journal folders
PATH_TO_JOURNALS = "./journals/"
fileNames = next(itertools.islice(os.walk(PATH_TO_JOURNALS), 0, None))[2]

# Store ALL locations of escaped ampersands so they can all be printed upon failure
errFileNames = []
errRows = []
errCols = []

for file in fileNames:
    if (file.endswith(".csv")):
        # For each .csv file in the folder, open in read mode
        with open(PATH_TO_JOURNALS + file, "r") as f:
            for i, line in enumerate(f):
                # For each line, if it has \&, store the fname, row and columns
                if ('\&' in line):
                    errFileNames.append(file)
                    errRows.append(i + 1)
                    errCols.append(
                        [index + 1 for index in range(len(line)) if line.startswith('\&', index)])


# In the case where we do find escaped &, the len() will be non-zero
if (len(errFileNames) > 0):
    err_msg = "["
    # For each file, append every row:col location to the error message
    for i, fname in enumerate(errFileNames):
        for col in errCols[i]:
            err_msg += "(" + fname + ", " + \
                str(errRows[i]) + ":" + str(col) + "), "
    # Format end of string and return as Value Error to 'fail' GitHub Actions process
    err_msg = err_msg[:len(err_msg) - 2]
    err_msg += "]"
    raise ValueError("Found Escaped Ampersands at: " + err_msg)
