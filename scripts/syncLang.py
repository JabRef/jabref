# coding=utf-8
import os
import re
import sys

res_dir = "src/main/resources/l10n"

keyFiles = {}


def get_keys_from_lines(lines):
    """
    Builds a list of all translation keys in the list of lines.

    :param lines: a list of strings
    :return: the sorted keys within the list of strings
    """
    keys = []
    for line in lines:
        comment = line.find("#")
        if comment != 0:
            index = line.find("=")
            while (index > 0) and (line[index - 1] == "\\"):
                index = line.find("=", index + 1)
            if index > 0:
                keys.append(line[0:index])
    return keys


def find_missing_keys(first_list, second_list):
    """
    Finds all keys in the first list that are not present in the second list

    :param first_list: a list
    :param second_list: a list
    :return: a list
    """
    missing = []
    for key in first_list:
        if key not in second_list:
            missing.append(key)
    return missing


def read_all_lines(filename):
    f1 = open(filename)
    lines = f1.readlines()
    f1.close()
    return lines


def append_keys_to_file(filename, keys):
    """
    Appends all the given keys to the file terminating with an equals sign
    """
    f = open(filename, "a")
    f.write("\n")
    for key in keys:
        f.write(key + "=\n")
    f.close()


def compare_property_files_to_main_property_file(main_properties_file, other_properties_files, append_missing_keys_to_other_properties_files):
    keys_in_properties_file = get_keys_from_lines(read_all_lines(main_properties_file))

    for other_properties_file in other_properties_files:
        keys_in_other_properties_file = get_keys_from_lines(read_all_lines(other_properties_file))
        keys_missing = find_missing_keys(keys_in_properties_file, keys_in_other_properties_file)
        keys_obsolete = find_missing_keys(keys_in_other_properties_file, keys_in_properties_file)

        print "\n\nFile '" + other_properties_file + "'\n"
        if not keys_missing:
            print "----> No missing keys."
        else:
            print "----> Missing keys:"
            for key in keys_missing:
                print key

            if append_missing_keys_to_other_properties_files:
                print "Update file?",
                if raw_input() in ['y', 'Y']:
                    append_keys_to_file(other_properties_file, keys_missing)
            print ""

        if not keys_obsolete:
            print "----> No possible obsolete keys (not in English language file)."
        else:
            print "----> Possible obsolete keys (not in English language file):"
            for key in keys_obsolete:
                print key
            print ""


def handleJavaCode(filename, lines, keyList):
    # Extract first string parameter from Localization.lang call. E.g., Localization.lang("Default")
    regex_only_key = r'"((\\"|[^"])*)"[^"]*'
    pattern_single_line = re.compile(r'Localization\s*\.\s*lang\s*\(\s*' + regex_only_key)
    pattern_only_key = re.compile(regex_only_key)

    # Find multiline Localization lang statements. E.g.:
    # Localization.lang("This is my string" +
    # "with a long text")
    pattern_multi_line = re.compile(r'Localization\s*\.\s*lang\s*\(([^)])*$')

    pattern_plus_symbol = re.compile(r'^\s*\+')

    lines_with_line_number = list(enumerate(lines))
    line_index = 0
    while line_index < len(lines_with_line_number):
        line_number, line = lines_with_line_number[line_index]

        # Remove Java single line comments
        if line.find("http://") < 0:
            line = re.sub("//.*", "", line)

        while line != "":
            result_single_line = pattern_single_line.search(line)
            result_multi_line = pattern_multi_line.search(line)

            found = ""

            if result_multi_line and line.find('",') < 0:
                # not terminated
                # but it could be a multiline string
                if result_single_line:
                    curText = result_single_line.group(1)
                    searchForPlus = True
                else:
                    curText = ""
                    searchForPlus = False
                origI = line_index
                # inspect next line
                while line_index + 1 < len(lines_with_line_number):
                    linenum2, curline2 = lines_with_line_number[line_index + 1]
                    if (not searchForPlus) or pattern_plus_symbol.search(curline2):
                        # from now on, we always have to search for a plus
                        searchForPlus = True

                        # The current line has been handled here, therefore indicate to handle the next line
                        line_index += 1
                        line_number = linenum2
                        line = curline2

                        # Search for the occurence of a string
                        result_single_line = pattern_only_key.search(curline2)
                        if result_single_line:
                            curText = curText + result_single_line.group(1)
                            # check for several strings in this line
                            if curline2.count('\"') > 2:
                                break
                            # check for several arguments in the line
                            if curline2.find('",') > 0:
                                break
                            if curline2.endswith(")"):
                                break
                        else:
                            # plus sign at the beginning found, but no string
                            break
                    else:
                        # no continuation found
                        break

                if origI == line_index:
                    print "%s:%d: Not terminated: %s" % (filename, line_number + 1, line)
                else:
                    found = curText

            if result_single_line or (found != ""):
                if found == "":
                    # not a multiline string, found via the single line matching
                    # full string in one line
                    found = result_single_line.group(1)

                found = found.replace(" ", "_")
                # replace characters that need to be escaped in the language file
                found = found.replace("=", r"\=").replace(":", r"\:")
                # replace Java-escaped " to plain "
                found = found.replace(r'\"', '"')
                # Java-escaped \ to plain \ need not to be converted - they have to be kept
                # e.g., "\\#" has to be contained as "\\#" in the key
                # found = found.replace('\\\\','\\')
                if (found != "") and (found not in keyList):
                    keyList.append(found)
                    keyFiles[found] = (filename, line_number)
                    # print "Adding ", found
                    # else:
                    #   print "Not adding: "+found

            # Prepare a possible second run (multiple Localization.lang on this line)
            if result_single_line:
                lastPos = result_single_line.span()[1]
                # regular expression is greedy. It will match until Localization.lang("
                # therefore, we have to adjust lastPos
                lastPos -= 14
                if len(line) <= lastPos:
                    line = ""
                else:
                    line = line[lastPos:]
            else:
                # terminate processing of this line, continue to next line
                line = ""

        line_index += 1


# Find all Java source files in the given directory, and read the lines of each,
# calling handleJavaCode on the contents:   
def handleDir(lists, directory_path, filenames):
    keyList, notTermList = lists
    for filename in filenames:
        if is_java_file(filename):
            file_path = directory_path + os.sep + filename
            lines = read_all_lines(file_path)
            handleJavaCode(file_path, lines, keyList)


def is_java_file(filename):
    """
    Determines whether a file name is that of a Java file

    :param filename: the filename to be checked
    :return: boolean
    """
    filename_length = len(filename)
    return filename_length > 6 and filename[(filename_length - 5):filename_length] == ".java"


# Go through subdirectories and call handleDir on all directories:
def traverseFileTree(starting_directory):
    keys = []
    notTermList = []
    os.path.walk(starting_directory, handleDir, (keys, notTermList))
    print "Keys found: " + str(len(keys))
    return keys


def append_property(properties_file, key, value):
    f = open(properties_file, "a")
    f.write(key + "=" + value + "\n")
    f.close()

def find_missing_and_obsolete_keys(properties_file, source_code_directory, append_missing_keys_to_properties_file):
    """
    Searches out all translation calls in the Java source files, and reports which
    are not present in the given resource file.

    :param properties_file: the properties file with the keys to sync with
    :param source_code_directory: the directory containing the source code to be checked
    :param append_missing_keys_to_properties_file: boolean whether the missing keys are appended to the properties_file
    """
    keys_in_property_file = get_keys_from_lines(read_all_lines(properties_file))
    keys_used_in_code = traverseFileTree(source_code_directory)

    keys_obsolete = find_missing_keys(keys_in_property_file, keys_used_in_code)
    keys_missing = find_missing_keys(keys_used_in_code, keys_in_property_file)

    if append_missing_keys_to_properties_file:
        f1 = open(properties_file, "a")
        f1.write("\n")
        f1.close()

    for key in keys_missing:
        value = key.replace("\\:", ":").replace("\\=", "=")
        file_name, line_number = keyFiles[key]
        print "%s:%i:Missing key: %s" % (file_name, line_number + 1, value)
        if append_missing_keys_to_properties_file:
            append_property(properties_file, key, value)

    for key in keys_obsolete:
        print "Possible obsolete key: " + key


def find_duplicate_keys_and_keys_with_no_value(current_file, display_keys):
    lines = read_all_lines(current_file)
    mappings = {}
    duplication_count = 0
    empty_values_count = 0
    for line in lines:
        is_no_comment = line.find("#") != 0
        index = line.find("=")
        contains_property = index > 0
        if is_no_comment and contains_property:
            key = line[0:index]
            value = line[index + 1:].strip()
            if key in mappings:
                mappings[key].append(value)
                duplication_count += 1
                if display_keys:
                    print "Duplicate: " + current_file + ": " + key + " =",
                    print mappings[key]
            else:
                mappings[key] = [value]
                if value == "":
                    empty_values_count += 1
                    if display_keys:
                        print "Empty value: " + current_file + ": " + key

    issues_count = duplication_count + empty_values_count

    message = ""
    if issues_count == 0:
        message = "ok"
    elif duplication_count > 0:
        message += str(duplication_count) + " duplicates. "
    elif empty_values_count > 0:
        message += str(empty_values_count) + " empty values. "
    print current_file + ": " + message


if len(sys.argv) == 1:
    print """This program must be run from the jabref base directory.
    
Usage: syncLang.py option   
Option can be one of the following:
 
    -c: Search the language files for empty and duplicate translations. Display only
        counts for duplicated and empty values in each language file.

    -d: Search the language files for empty and duplicate translations. 
        For each duplicate set found, a list will be printed showing the various 
        translations for the same key. There is currently to option to remove duplicates
        automatically.
        
    -s [-u]: Search the Java source files for language keys. All keys that are found in the source files
        but not in "JabRef_en.properties" are listed. If the -u option is specified, these keys will
        automatically be added to "JabRef_en.properties".
        
        The program will also list "Not terminated" keys. These are keys that are concatenated over 
        more than one line, that the program is not (currently) able to resolve.
        
        Finally, the program will list "Possible obsolete keys". These are keys that are present in
        "JabRef_en.properties", but could not be found in the Java source code. Note that the 
        "Not terminated" keys will be likely to appear here, since they were not resolved.
        
    -t [-u]: Compare the contents of "JabRef_en.properties" and "Menu_en.properties" against the other 
        language files. The program will list for all the other files which keys from the English
        file are missing. Additionally, the program will list keys in the other files which are
        not present in the English file - possible obsolete keys.
        
        If the -u option is specified, all missing keys will automatically be added to the files.
        There is currently no option to remove obsolete keys automatically.
"""

elif (len(sys.argv) >= 2) and (sys.argv[1] == "-s"):
    if (len(sys.argv) >= 3) and (sys.argv[2] == "-u"):
        update = True
    else:
        update = False
    find_missing_and_obsolete_keys(os.path.join(res_dir, "JabRef_en.properties"), ".", update)

elif (len(sys.argv) >= 2) and (sys.argv[1] == "-t"):
    if (len(sys.argv) >= 3) and (sys.argv[2] == "-u"):
        change_files = True
    else:
        change_files = False

    filesJabRef = filter(lambda s: (s.startswith('JabRef_') and not (s.startswith('JabRef_en'))), os.listdir(res_dir))
    filesJabRef = [os.path.join(res_dir, i) for i in filesJabRef]
    filesMenu = filter(lambda s: (s.startswith('Menu_') and not (s.startswith('Menu_en'))), os.listdir(res_dir))
    filesMenu = [os.path.join(res_dir, i) for i in filesMenu]

    compare_property_files_to_main_property_file(os.path.join(res_dir, "JabRef_en.properties"), filesJabRef, change_files)
    compare_property_files_to_main_property_file(os.path.join(res_dir, "Menu_en.properties"), filesMenu, change_files)

elif (len(sys.argv) >= 2) and ((sys.argv[1] == "-d") or (sys.argv[1] == "-c")):
    files = filter(lambda s: (s.startswith('JabRef_') and not (s.startswith('JabRef_en'))), os.listdir(res_dir))
    files.extend(filter(lambda s: (s.startswith('Menu_') and not (s.startswith('Menu_en'))), os.listdir(res_dir)))
    files = [os.path.join(res_dir, i) for i in files]
    for f in files:
        find_duplicate_keys_and_keys_with_no_value(f, sys.argv[1] == "-d")
