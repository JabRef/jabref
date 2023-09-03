
import pandas as pd
import_order = [
    '../journals/journal_abbreviations_acs.csv',
    '../journals/journal_abbreviations_aea.csv',
    '../journals/journal_abbreviations_ams.csv',
    '../journals/journal_abbreviations_annee-philologique.csv',
    '../journals/journal_abbreviations_astronomy.csv',
    '../journals/journal_abbreviations_dainst.csv',
    '../journals/journal_abbreviations_entrez.csv',
    '../journals/journal_abbreviations_geology_physics.csv',
    '../journals/journal_abbreviations_geology_physics_variations.csv',
    '../journals/journal_abbreviations_ieee.csv',
    '../journals/journal_abbreviations_ieee_strings.csv',
    '../journals/journal_abbreviations_lifescience.csv',
    '../journals/journal_abbreviations_mathematics.csv',
    '../journals/journal_abbreviations_mechanical.csv',
    '../journals/journal_abbreviations_medicus.csv',
    '../journals/journal_abbreviations_meteorology.csv',
    '../journals/journal_abbreviations_sociology.csv',
    '../journals/journal_abbreviations_webofscience-dotless.csv',
    '../journals/journal_abbreviations_webofscience-dots.csv'
]


def handle_bad_line(line):
    print("Handle the problematic line manually:", line)


# read the csv files into dataframes
file_in = "../journals/journal_abbreviations_general.csv"
general = pd.read_csv(file_in, delimiter=',', header=None, names=["Title", "abbreviation", "ShortestAbbreviation", "frequency"], dtype={
                      "Title": str, "abbreviation": str, "ShortestAbbreviation": str})
# Creating a new column Title lc which is Title in lower case for case insensitive comparison
general['Title_lc'] = general['Title'].str.lower()

dflist = []
for filename in import_order:
    df = pd.read_csv(filename, delimiter=',', on_bad_lines=handle_bad_line, engine='python', names=[
                     "Title", "abbreviation", "ShortestAbbreviation", "frequency"], dtype={"Title": str, "abbreviation": str, "ShortestAbbreviation": str})
    dflist.append(df)

non_general_csv_df = pd.concat(dflist, ignore_index=True)

# Remove duplicates from non_general_csv_df to avoid removing valid entries
non_general_csv_df.drop_duplicates(
    subset=['Title'], inplace=True, keep='first')
# Creating a new column Title lc which is Title in lower case for case insensitive comparison
non_general_csv_df['Title_lc'] = non_general_csv_df['Title'].str.lower()

# Merge the two dataframes on only the Title in lower case column
merged_df = pd.merge(general, non_general_csv_df,
                     on='Title_lc', how='left', indicator=True)

# Keep only the rows that are present in general but not in non_general_csv_df
result_df = merged_df.loc[merged_df['_merge'] == 'left_only', ['Title_lc']]

result_df = pd.merge(general[['Title', 'abbreviation', 'ShortestAbbreviation',
                     'Title_lc']], result_df, on='Title_lc', how='inner')
# Dropping the newly added column only used for comparison
result_df.drop('Title_lc', axis=1, inplace=True)
# Save the result dataframe to a csv file
result_df.to_csv(file_in,  header=None, index=None, sep=',')
