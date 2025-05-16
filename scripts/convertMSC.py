# Converts MSC Code PDF into a JSON file

import re
import PyPDF2
import unicodedata
import json
import sys
import math


def extract_text_from_pdf(pdf_path, start_page=0, end_page=None):
    """
    Extract text from a PDF file with page range limits.

    Args:
        pdf_path (str): Path to the PDF file
        start_page (int): First page to extract (0-indexed)
        end_page (int): Last page to extract (exclusive), None for all pages

    Returns:
        str: Extracted text from the specified PDF pages
    """
    text = ""
    try:
        with open(pdf_path, 'rb') as file:
            pdf_reader = PyPDF2.PdfReader(file)
            total_pages = len(pdf_reader.pages)

            if start_page < 0:
                start_page = 0
            if end_page is None or end_page > total_pages:
                end_page = total_pages

            #print(f"\nExtracting text from pages {start_page+1} to {end_page} (of {total_pages} total pages)\n")

            # get from specified pages only
            for page_num in range(start_page, end_page):
                page = pdf_reader.pages[page_num]
                text += page.extract_text()
                text += "\n"  # page separator



        return text
    except Exception as e:
        print(f"Error extracting text from PDF: {e}")
        return ""


def clean_pdf_text(text):
    """
    Clean text extracted from PDF and replace unicodes with symbols.

    Args:
        text (str): code description that needs to be sanitized

    Return:
        text (str): string with symbols added
    """
    # ä, ë, ï, ö, ü, ÿ,
    # Ä, Ë, Ï, Ö, Ü, Ÿ
    # â, ê, î, ô, û
    # Â, Ê, Î, Ô, Û
    # à, è, ì, ò, ù,
    # À, È, Ì, Ò, Ù ≥

    text = text.replace('\f', 'fi')
    text = text.replace(",", "")
    text = text.replace('\r;\u000e', 'gamma; delta')
    text = text.replace('\r', 'fl')
    text = text.replace('\u000b', 'ff')
    text = text.replace('\u000f', 'ff')
    text = text.replace('\u000e', 'ffi')
    text = text.replace('\u0013 c', 'ć')
    text = text.replace('\u0015 \u0010', 'ĭ') #ĭ
    text = re.sub(r'fFor (.+?) g', r'[For \1]', text)
    text = re.sub(r'fFor (.+?)$', r'[For \1]', text, flags=re.MULTILINE)
    text = text.replace(' o','ö')
    text = text.replace(' u','ü')
    text = text.replace(' a','ä')
    text= text.replace('\u0010', '\zeta')
    text= text.replace('\u001f', '\chi')
    text= text.replace('\u001b', 'σ')
    text= text.replace('\u001e', 'φ')
    text= text.replace('\u0003', '*')
    text= text.replace('\u0012 e', 'è') #è
    text= text.replace('\u0012 a', 'à') #à
    text= text.replace('\u0013 a', 'à') #à
    text= text.replace('\u0013 E', 'É') #É
    text= text.replace('\u0013E', 'É') #É
    text= text.replace('\u0013 e', 'é') #é
    text= text.replace('\u0013 o', 'ó') #ó
    text= text.replace('\u0013 y', 'ý') #ý
    text= text.replace('\u0013 Á', 'Á') #Á
    text= text.replace('\u0013 I', 'Í') #Í
    text= text.replace('\u0013 O', 'Ò') #Ò
    text= text.replace('\u0013 U', 'Ù') #Ù
    text= text.replace('\u0013 n', 'ń') #ń
    text= text.replace('\u0014C', 'ˇC') #ˇC
    text= text.replace('\u0014A', 'ˇA') #ˇA
    text= text.replace('\u0014E', 'ˇE') #ˇE
    text= text.replace('\u0014I', 'ˇI') #ˇI φ
    text= text.replace('\u0014', '≤') #≥ ≤
    text= text.replace('\u0015', '≥') #≥ ≤
    text= text.replace('\u0019', 'π')#π
    text= text.replace('\u0000', '-')#-
    text= text.replace('\u001a', ' ⊂ ')# ⊂


    return text

def fix_code_specific(code):
    """
    returns description for codes that weren't properly sanitized by PyPDF

    Args:
        code (str) that was matched by regex

    Returns:
         - description (str): correct description without any commas
         - -1: if otherwise
    """
    match code:
        case "03":
            return "Mathematical logic and foundations"
        case "11F30":
            return "Fourier coefficients of automorphic forms"
        case "13-XX":
            return "Commutative algebra"
        case "15A66":
            return "Clifford algebras spinors"
        case "16G30":
            return "Representations of orders lattices algebras over commutative rings [See also 16Hxx]"
        case "16W80":
            return "Topological and ordered rings and modules [See also 06F25 13Jxx]"
        case "19-XX":
            return "K-theory"
        case "19A22":
            return "Frobenius induction Burnside and representation rings"
        case "20G05":
            return "Representation theory for linear algebraic groups"
        case "22E40":
            return "Discrete subgroups of Lie groups [See also 20Hxx 32Nxx]"
        case "26E70":
            return "Real analysis on time scales or measure chains {For dynamic equations on time scales or measure chains see 34N05}"
        case "37-XX":
            return "Dynamical systems and ergodic theory"
        case "43A20":
            return "L1-algebras on groups semigroups etc."
        case "49N80":
            return "Mean field games and control {For partial differential equations see 35Q89; for game theory see 91A16}"
        case "49Q15":
            return "Geometric measure and integration theory integral and normal currents in optimization [See also 28A75 32C30 58A25 58C35]"
        case "65D18":
            return "Numerical aspects of computer graphics image analysis and computational geometry [See also 51N05 68U05]"
        case "68Q06":
            return "Networks and circuits as models of computation; circuit complexity [See also 94C11]"
        case "74-XX":
            return "Mechanics of deformable solids"
        case "74A15":
            return "Thermodynamics in solid mechanics"
        case "93DXX":
            return "Stability of control systems"
        case "32C30":
            return "Integration on analytic sets and spaces currents [See also 32A25 32A27]"
        case "32Qxx":
            return "Complex manifolds"
        case "11Txx":
            return "Finite fields and commutative rings (number-theoretic aspects)"
        case "03E72":
            return "Theory of fuzzy sets"
        case _:
            return -1

def is_page_num(text):
    if (int(text) <= 300 and len(text)<= 3):
        return True
    return False

def validate_unique_pairs(msc_dict):
    """
    Validates that all code-description pairs are unique (1:1 relationship).

    Args:
        msc_dict (dict): Dictionary with MSC codes as keys and descriptions as values

    Returns:
        tuple: (is_valid, issues)
            - is_valid (bool): True if all pairs are unique, False otherwise
            - issues (dict): Dictionary with information about non-unique pairs
    """
    is_valid = True
    issues = {
        "duplicate_codes": [],
        "duplicate_descriptions": []
    }

    # create reverse mapping (description -> [codes])
    desc_to_codes = {}
    for code, desc in msc_dict.items():
        if desc not in desc_to_codes:
            desc_to_codes[desc] = []
        desc_to_codes[desc].append(code)

    # check for descriptions that map to multiple codes
    for desc, codes in desc_to_codes.items():
        if len(codes) > 1:
            is_valid = False
            issues["duplicate_descriptions"].append({
                "description": desc,
                "codes": codes
            })

    return is_valid, issues

def report_validation_issues(issues):
    """
    Reports validation issues found in the MSC code-description pairs.

    Args:
        issues (dict): Dictionary with information about non-unique pairs
    """
    if not issues["duplicate_descriptions"]:
        print("✓ All descriptions are unique")
    else:
        print(f"✗ Found {len(issues['duplicate_descriptions'])} descriptions with multiple codes:")
        for issue in issues["duplicate_descriptions"]:
            desc = issue["description"]
            codes = ", ".join(issue["codes"])
            # truncate long descriptions for display
            if len(desc) > 50:
                desc = desc[:47] + "..."
            print(f"  - \"{desc}\" is linked to codes: {codes}")



def parse_msc_codes(pdf_path):
    """Parse Mathematics Subject Classification (MSC) codes from a PDF file."""

    msc_dict = {}

    content = extract_text_from_pdf(pdf_path, 3, 224)

    if not content:
        print("Failed to extract text from PDF or the PDF is empty.")
        return msc_dict

    content = clean_pdf_text(content)

    # identify all MSC codes
    code_pattern = r'\n(\d{2}[A-Z]?(?:-[A-Z]{2}|-\d{2}|\d{2}|[A-Z]{2}|[a-z]{2})?)\s+'
    code_positions = [(m.group(1), m.start()) for m in re.finditer(code_pattern, content)]

    code_positions.append(("END", len(content)))

    # extract full description
    for i in range(len(code_positions) - 1):
        code = code_positions[i][0]
        start_pos = code_positions[i][1]
        end_pos = code_positions[i+1][1]

        section_text = content[start_pos:end_pos].strip()

        if(len(code) <= 2 and is_page_num(code)):
            code = section_text.split(' ')[0].split('\n')[1] # used to replace page number with code
        if(not code.endswith("99")):
                description =' '.join(section_text.split(' ')[1:]) # remove code from description
        else:
            # `None of above but in this section` is a common duplicated description
            # to ensure 1:1 relationship we leave the code it pertains to
            description = section_text

        if(code == "57N45"): # handle specific issue with 57N45 desc containing 57N50
            description, description2 = description.split('57N50')[0], section_text.split('57N50')[1]
            msc_dict["57N50"] = description2

        description = description.replace('\n', ' ').strip()

        if not description.startswith("[See") and not description.startswith("For"):

            if fix_code_specific(code) != -1:
                description = fix_code_specific(code)

            msc_dict[code] = description
            print(f"\rFOUND {len(msc_dict)} MSC codes with descriptions", end="", flush=True)

    return msc_dict

def make_desc_unique(dict, issues):
    """makes descriptions unique"""
    for issue in issues["duplicate_descriptions"]:
            # desc = issue["description"]
            codes = issue["codes"]
            # Truncate long descriptions for display
            i = 0
            for code in codes:
                dict[code] += ("*" * i)
                i += 1
    return dict



def main():
    if(len(sys.argv) < 2):
       print("Usage ./convert.py <path-to-pdf>")
       exit()

    pdf_path = sys.argv[1]

    if(not '.pdf' in pdf_path):
        print(f"Expected pdf file, received {pdf_path}")
        exit()

    # parse the file
    msc_dict = parse_msc_codes(pdf_path)
    print()

    is_valid, issues = validate_unique_pairs(msc_dict)
    msc_dict = make_desc_unique(msc_dict, issues)
    is_valid, issues = validate_unique_pairs(msc_dict)

    if(is_valid):
        print(f"Are values unique: {is_valid}")
    else:
        report_validation_issues(issues)

    # save the dictionary to a file
    with open('msc_codes.json', 'w', encoding='utf-8') as f:
        json.dump(msc_dict, f, ensure_ascii=False, indent=2)
    print("Dictionary saved to 'msc_codes.json'")

if __name__ == "__main__":
    main()
