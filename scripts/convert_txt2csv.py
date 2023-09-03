#!/usr/bin/python3

"""
Python script for converting journals abbreviation files to CSV format.

Dependencies:
pandas

Usage:
python3 convert_txt2csv.py
"""

import glob
import os
import pandas as pd
import csv

for file in glob.glob("journals/*.txt"):
    fileName, _fileExtension = os.path.splitext(file)
    commented_lines = 0
    with open(file) as f:
        for line in f:
            if not line.strip().startswith("#") and line.strip():
                separator = " = " if " = " in line else "="
                break
            commented_lines += 1
    df = pd.read_csv(fileName + ".txt", sep=separator, skiprows=commented_lines, header=None,
                     engine="python", skipinitialspace=True, index_col=0, names=["Name", "Abbrev"])
    df.index = df.index.str.strip()
    df = df.Abbrev.str.split(",", expand=True)
    df.to_csv(fileName + ".csv", sep=",", header=False, quoting=csv.QUOTE_ALL)
    print(fileName + ".csv")
