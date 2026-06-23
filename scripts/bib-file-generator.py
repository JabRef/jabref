# Creates "large-library.bib" with 100k entries.

# The file is written in UTF8 and makes use of the unicode character U+0304 (https://www.compart.com/en/unicode/U+0304)
# to create an overline on large roman numbers using the technicue "Vinculum" (https://en.wikipedia.org/wiki/Roman_numerals#Vinculum).
# The numbers are used in the journal title.

# For pseudonymization BibTeX files, org.jabref.logic.pseudonymization.PseudonymizationTest#pseudonymizeLibraryFiley can be used.

number_of_entries = 100_000

# Adapted from: https://stackoverflow.com/a/50012689/873282
def int_to_roman(num):
    _values = [
        1000000, 900000, 500000, 400000, 100000, 90000, 50000, 40000, 10000, 9000, 5000, 4000, 1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1]

    _strings = [
        'M', 'CM', 'D', 'CD', 'C', 'XC', 'L', 'XL', 'X', 'IX', 'V', 'IV', "M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"]

    result = ""
    decimal = num

    while decimal > 0:
        for i in range(len(_values)):
            if decimal >= _values[i]:
                if _values[i] > 1000:
                    result += u'\u0304'.join(list(_strings[i])) + u'\u0304'
                else:
                    result += _strings[i]
                decimal -= _values[i]
                break
    return result

with open("generated-large-library.bib", 'w', encoding='utf-8') as file:
    for i in range(1, number_of_entries + 1):
        year = 1900 + (i - 1) % (2025 - 1900)
        entry = f"""@article{{id{i:06d},
  title   = {{This is my title{i}}},
  author  = {{FirstnameA{i} LastnameA{i} and FirstnameB{i} LastnameB{i} and FirstnameC{i} LastnameC{i}}},
  journal = {{Journal Title {int_to_roman(i)}}},
  volume  = {{{i}}},
  year    = {{{year}}},
}}

"""
        file.write(entry)
