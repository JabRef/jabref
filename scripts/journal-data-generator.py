import urllib3
import json 

http = urllib3.PoolManager()

crossref_base_url = 'https://api.crossref.org/journals'

def crossref_with_ISSN(ISSN):
    url = crossref_base_url + '/' + ISSN
    print(url)
    return http.request('GET', url)  

# Test endpoint: https://api.crossref.org/journals/2158-5571 
response = crossref_with_ISSN('2158-5571')
print(response.status)
# print(type(response.data))

data = json.loads(response.data.decode('utf-8'))

with open('data.json', 'w') as json_file:
  json.dump(data, json_file)

