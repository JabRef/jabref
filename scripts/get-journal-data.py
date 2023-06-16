import urllib.request
import csv
import os
import json

current_year = 2022  # update this year for future runs
start_year = 1999
data_directory = 'data'


def journal_url(year):
    return f'https://www.scimagojr.com/journalrank.php?year={year}&out=xls'


def parse_float(str):
    try:
        float_val = float(str.replace(',', '.'))
        return float_val
    except ValueError:
        return 0.0


def parse_int(str):
    try:
        int_val = int(str)
        return int_val
    except ValueError:
        return 0


def download_all_data():
    for year in range(start_year, current_year+1):
        print(f'Downloading data for {year}')
        url = journal_url(year)
        filename = f'scimagojr-journal-{year}.csv'
        urllib.request.urlretrieve(url, os.path.join(data_directory, filename))


def get_combined_data():
    download_all_data()

    # iterate over files and build consolidated dataset
    journals = {}
    for year in range(start_year, current_year+1):
        print(f'Processing {year}')
        filename = f'scimagojr-journal-{year}.csv'
        with open(os.path.join(data_directory, filename), mode='r') as csv_file:
            csv_reader = csv.DictReader(csv_file, delimiter=';')
            for row in csv_reader:
                # Columns present in the csv:
                # 'Rank', 'Sourceid', 'Title', 'Type', 'Issn', 'SJR', 'SJR Best Quartile', 'H index',
                # 'Total Docs. (2020)', 'Total Docs. (3years)', 'Total Refs.', 'Total Cites (3years)',
                # 'Citable Docs. (3years)', 'Cites / Doc. (2years)', 'Ref. / Doc.', 'Country', 'Region',
                # 'Publisher', 'Coverage', 'Categories', 'Areas'

                issn = row['Issn']
                hIndex = parse_int(row['H index'])
                sjr = parse_float(row['SJR'])
                rank = parse_int(row['Rank'])
                totalDocs = parse_int(row[f'Total Docs. ({year})'])
                totalDocs3Years = parse_int(row['Total Docs. (3years)'])
                totalRefs = parse_int(row['Total Refs.'])
                totalCites3Years = parse_int(row['Total Cites (3years)'])
                citableDocs3Years = parse_int(row['Citable Docs. (3years)'])
                citesPerDoc2Years = parse_float(row['Cites / Doc. (2years)'])
                refPerDoc = parse_float(row['Ref. / Doc.'])

                if issn not in journals:
                    journals[issn] = get_default_entry()
                    # populate non-varying fields
                    entry = journals[issn]
                    entry['sourceId'] = row['Sourceid']
                    entry['title'] = row['Title']
                    entry['type'] = row['Type']
                    entry['issn'] = issn
                    entry['country'] = row['Country']
                    entry['region'] = row['Region']
                    entry['publisher'] = row['Publisher']
                    entry['coverage'] = row['Coverage']
                    entry['categories'] = row['Categories']
                    entry['areas'] = row['Areas']

                # populate yearly varying fields
                entry = journals[issn]
                entry['rank'].append({'x': year, 'y': rank})
                entry['sjr'].append({'x': year, 'y': sjr})
                entry['hIndex'].append({'x': year, 'y': hIndex})
                entry['totalDocs'].append({'x': year, 'y': totalDocs})
                entry['totalDocs3Years'].append(
                    {'x': year, 'y': totalDocs3Years})
                entry['totalRefs'].append({'x': year, 'y': totalRefs})
                entry['totalCites3Years'].append(
                    {'x': year, 'y': totalCites3Years})
                entry['citableDocs3Years'].append(
                    {'x': year, 'y': citableDocs3Years})
                entry['citesPerDoc2Years'].append(
                    {'x': year, 'y': citesPerDoc2Years})
                entry['refPerDoc'].append({'x': year, 'y': refPerDoc})

    # write to json file
    print('Writing to json')
    with open('scimagojr_combined_data.json', 'w') as fp:
        json.dump(journals, fp)

    print(f'Number of journals collected: {len(journals)}')


def get_default_entry():
    return {
        'sourceId': '',
        'title': '',
        'type': '',
        'issn': '',
        'country': '',
        'region': '',
        'publisher': '',
        'coverage': '',
        'categories': '',
        'areas': '',
        'rank': [],
        'sjr': [],
        'hIndex': [],
        'totalDocs': [],
        'totalDocs3Years': [],
        'totalRefs': [],
        'totalCites3Years': [],
        'citableDocs3Years': [],
        'citesPerDoc2Years': [],
        'refPerDoc': [],
    }


get_combined_data()
