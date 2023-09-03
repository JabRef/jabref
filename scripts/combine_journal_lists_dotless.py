#!/usr/bin/env python3

"""
Python script for combining several journal abbreviation lists
and producing an alphabetically sorted list. If the same journal
names are repeated, only the version found last is retained.

This version of the script specifically combines the lists following the ISO4
standard WITHOUT dots after abbreviated words.

Usage: combine_journal_lists.py
Input: see list of files below
Output: writes file 'journalList_dotless.csv'
"""

import sys
import pandas as pd

# Define the list of CSV files
import_order = [
    'journals/journal_abbreviations_entrez.csv',
    'journals/journal_abbreviations_medicus.csv',
    'journals/journal_abbreviations_webofscience-dotless.csv'
]


def main(output_filename):
    # Read and merge CSV files
    # dfs = [pd.read_csv(file, header=None) for file in import_order]
    dfs = []
    for file in import_order:
        df = pd.read_csv(file, header=None)
        dfs.append(df)
        print(f"{file}: {len(df)}")
    merged_df = pd.concat(dfs, ignore_index=True)

    # Drop duplicates based on the first column value and keep the last one obtained
    merged_df.drop_duplicates(subset=[0], keep='last', inplace=True)

    # Sort alphabetically
    sorted_df = merged_df.sort_values(by=[0])

    # Save the result to the specified CSV file and ensure values are quoted
    sorted_df.to_csv(output_filename, index=False, header=False, quoting=1)

    print(f"Write {output_filename}, Combined key count: {len(merged_df)}")


if __name__ == "__main__":
    if len(sys.argv) > 1:
        filename = sys.argv[1]
    else:
        filename = "journalList_dotless.csv"

    main(filename)
