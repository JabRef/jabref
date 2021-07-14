import urllib.request as rq
import sys

def journal_url(year):
    # Search view: https://www.scimagojr.com/journalrank.php?year=2019
    baseURL = 'https://www.scimagojr.com/journalrank.php?year='
    # '&out=xls' returns a .csv-file with the journal rankings of the specified year
    downloadQuery = '&out=xls'
    return baseURL + str(year) + downloadQuery

def get_year_stats(year, issn):
    response = rq.urlopen(journal_url(year))
    # Response Status 200 OK
    if response.status == 200:
        lines = response.read().splitlines()
        for l in lines:
            decoded = l.decode('utf-8')
            cells = decoded.split(';')
            if len(cells) < 5:
                continue # incomplete data
            # cells[4] should contain a list of ISSNs for the journal, separated by ', '
            # e.g. "12345678, 98765432"
            if issn in cells[4].split(', '):
                return str(year) + ";" + decoded # prepend the year

        return [] # TODO: handle case where ISSN is not found
    else:
        return [] # TODO: revise potential HTTP error handling
        
# which journal the data is fetched for
journal_issn = '15458601' # default for testing
issn = sys.argv[1] if len(sys.argv) > 1 else journal_issn

# range of available years as of 2021-03-08
start_year = 1999
end_year = 2019
years = range(start_year, end_year+1)

journal_stats = []

for y in years:
    journal_stats.append(get_year_stats(y, issn))

# TODO:
# write aggregated stats to .json-file