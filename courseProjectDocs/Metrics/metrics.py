import os 
import re
import glob 

import lizard
from pygount import SourceAnalysis
import csv

# 1. LOC (per file/module)
# 2. comment density : comments / total lines 
# 3. cyclomatic complexity


standalone_paths = []

def print_results(results):
    with open('results.csv', 'w', newline='') as csvfile:
        fieldnames = ['Module', 'loc', 'density', 'complexity']
        writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
        writer.writeheader()
        for result in results:
            writer.writerow(result)

def get_all_src_paths(root_dir): # This function grabs all paths that have 'src' for modules to count metrics.
    path_list = []
    for path, names, file in os.walk(root_dir):
        for name in names:
            if 'src' in name: 
                full_path = os.path.join(path, name)
                path_list.append(full_path)
                dir_name = os.path.dirname(full_path)
                standalone_paths.append(os.path.basename(dir_name))
    return path_list

def get_all_files(directory):
    all_files = []
    for root, dirs, files in os.walk(directory):
        for file in files:
            if '.' not in file:
                continue # Exclude any file without file extension (verfied txt files no major contribution to src code)
            file_path = os.path.join(root, file)
            abs_path = os.path.abspath(file_path)
            all_files.append(abs_path)
    return all_files

def evaluate_files(files, index):
    total_loc = 0 
    total_comments = 0
    total_density = 0.0 
    total_cc = 0
    for file in files:
        i = lizard.analyze_file(file) 
        loc = i.__dict__.get("nloc") # typeof int
        analyze = SourceAnalysis.from_file(file, "pygount") 
        comments = analyze.documentation_count # typeof int
        big_cc = 0
        if i.function_list:
            for func in i.function_list:
                cc = func.__dict__.get('cyclomatic_complexity')
                big_cc = big_cc + cc
        total_cc = big_cc + total_cc
        total_loc = loc + total_loc 
        total_comments = comments + total_comments
    try:
        total_density = total_loc / total_comments
    except ZeroDivisionError:
        total_density = -1 # Module has 0 LOC?? check.
    file_dict = {
        'Module':standalone_paths[index], 'loc':total_loc, 'density':total_density, 'complexity':total_cc
    }
    return file_dict

def main():
    root_dir = os.path.abspath(os.curdir) # glob_pattern = "/.*/src/*.java" or ".kts" (java or kotlin)
    paths = get_all_src_paths(root_dir)  #  args_ignore = ".*"
    exclude_exts = ('.tff', '.xsl', '.fxml', '.properties', '.blg', '.gitignore', '.tex', '.layout',
                    '.md', '.terms', '.readme', '.json', '.jstyle', '.jks', '.aux', '.csl', '.bst',
                    '.cff', '.enw', '.ctv6bak', '.txt', '.isi', '.nbib', '.ris', '.ref', '.docx', 
                    '.g4', '.gitkeep', '.sh', '.ico', '.icns', '.svo', '.Writer', '.ResourceLocator', 
                    '.IkonHandler', '.IkonProvider', '.ttf', '.bak', '.end', '.log', '.bib', '.pdf', 
                    '.png', '.http', '.xml') # List of ext to exclude
    results = []
    print("Program now running .... ")
    for i, dir in enumerate(paths): 
        single_dir = get_all_files(dir)
        filtered_files = [f for f in single_dir if not f.endswith(exclude_exts)] 
        result = evaluate_files(filtered_files, i)
        results.append(result)
        print(str(result))
    print_results(results)
    print("Program done!") #    print(results)

if __name__ == "__main__":
    main()
