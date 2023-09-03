#!/usr/bin/env python3

import pandas as pd
import csv

file_in = "https://mathscinet.ams.org/msnhtml/annser.csv"
file_out = "journals/journal_abbreviations_mathematics.csv"

# Get the first two fields of the last version of MathSciNet data file, without empty values
df_new = pd.read_csv(file_in, usecols=[0, 1]).dropna()[
    ["Full Title", "Abbrev"]]

# Get our last mathematics data file
df_old = pd.read_csv(file_out, sep=",", escapechar="\\",
                     header=None, names=["Full Title", "Abbrev"])

# Concatenate, remove duplicates and sort by journal name
df = pd.concat([df_new, df_old], axis=0).drop_duplicates(
).sort_values(by=["Full Title", "Abbrev"])

# Remove values where journal name is equal to abbreviation
df = df[df["Full Title"].str.lower() != df["Abbrev"].str.lower()]

# Save the end file in the same path as the old one
df.to_csv(file_out, sep=",", escapechar="\\", index=False, header=False, quoting=csv.QUOTE_ALL)
