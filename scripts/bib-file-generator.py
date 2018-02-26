number_of_entries = 100000
file = open("generatedDatabase.bib", 'w')

for i in range(number_of_entries):
    entry = """@article{%i,
    author  = {%i},
    title   = {%i},
    journal = {%i},
    volume  = {%i},
    year    = {%i},
    pages   = {%i},
 }""" % (i, i, i, i, i, i, i)
    file.write(entry)
file.flush()
file.close()