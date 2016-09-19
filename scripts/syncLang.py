# coding=utf-8
import os
import sys

res_dir = "src/main/resources/l10n"

keyFiles = {}


def enum(**enums):
    return type('Enum', (), enums)

OUTPUT_COLORS = enum(OK='\033[0;32m', FAIL='\033[0;31m', ENDC='\033[0;37m')


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

def remove_keys_from_file(filename, keys):
    lines = open(filename).readlines()
    lines_to_write = []
    for line in lines:
        add = True
        for key in keys:
            if(line.startswith(key+"=")):
                add = False
        if add:
            lines_to_write.append(line)
    open(filename, 'w').writelines(lines_to_write)


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
                append_keys_to_file(other_properties_file, keys_missing)
            print ""

        if not keys_obsolete:
            print "----> No possible obsolete keys (not in English language file)."
        else:
            print "----> Possible obsolete keys (not in English language file):"
            for key in keys_obsolete:
                print key

            if append_missing_keys_to_other_properties_files:
                remove_keys_from_file(other_properties_file, keys_obsolete)
                
            print ""


def append_property(properties_file, key, value):
    f = open(properties_file, "a")
    f.write(key + "=" + value + "\n")
    f.close()


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


def has_duplicate_keys(keys_to_check):
    """
    Checks if the property lines has duplicates

    :param lines: a list of Strings
    :return: a boolean
    """
    keys_checked = []
    for key in keys_to_check:
        if key in keys_checked:
            return True
        else:
            keys_checked.append(key)
    return False


def get_key_from_line(line):
    """
    Tries to extract the key from the line

    :param line: a String
    :return: the key (String) or None
    """
    comment_line = line.find("#")
    if comment_line != 0:
        index_key_end = line.find("=")
        while (index_key_end > 0) and (line[index_key_end - 1] == "\\"):
            index_key_end = line.find("=", index_key_end + 1)
        if index_key_end > 0:
            return line[0:index_key_end]
    return None


def get_keys_dict(property_lines):
    """
    Saves all the localizations (the whole String 'translationKey=translation')
    in a dict with their translationKey as the dict key

    :param property_lines: a list
    :return: a dict
    """
    keys = {}
    for line in property_lines:
        key = get_key_from_line(line)
        if key is not None:
            keys[key] = line
    return keys


def sort_property_files_to_main_property_file(main_properties_file, other_property_file):
    """
    Sorts the properties in the second property file in accordance of the first one

    :param main_properties_file:
    :param other_property_file:
    :return:
    """
    lines_in_property_file = read_all_lines(main_properties_file)
    keys_in_property_file = get_keys_from_lines(lines_in_property_file)

    for other_property_file in other_property_file:
        lines_in_other_property_file = read_all_lines(other_property_file)
        keys_in_other_property_file = get_keys_from_lines(lines_in_other_property_file)
        keys_missing = find_missing_keys(keys_in_property_file, keys_in_other_property_file)
        keys_obsolete = find_missing_keys(keys_in_other_property_file, keys_in_property_file)
        other_has_duplicate_keys = has_duplicate_keys(keys_in_other_property_file)

        error_message = ""
        if len(keys_missing) > 0:
            error_message += " has missing keys;"
        if len(keys_obsolete) > 0:
            error_message += " has obsolete keys;"
        if other_has_duplicate_keys:
            error_message += " has duplicate keys;"
        if len(error_message) > 0:
            print "{color_fail}{file}:{message} skip file{color_end}" \
                    .format(color_fail=OUTPUT_COLORS.FAIL, file=other_property_file, message=error_message, color_end=OUTPUT_COLORS.ENDC)
            continue

        other_lines_to_write = []
        other_lines_key_map = get_keys_dict(lines_in_other_property_file)
        for line in lines_in_property_file:
            key = get_key_from_line(line)
            if key is not None:
                line_to_write = other_lines_key_map[key]
                if not line_to_write.endswith("\n"):
                    line_to_write += "\n"
                other_lines_to_write.append(line_to_write)
            else:
                other_lines_to_write.append(line)
        open(other_property_file, "w").writelines(other_lines_to_write)

        print "{color_ok}{file} has been sorted successfully{color_end}" \
                .format(color_ok=OUTPUT_COLORS.OK, file=other_property_file, color_end=OUTPUT_COLORS.ENDC)


if len(sys.argv) == 1:
    print """This program must be run from the jabref base directory.
    
Usage: syncLang.py option   
Option can be one of the following:
 
    -c: Search the language files for empty and duplicate translations. Display only
        counts for duplicated and empty values in each language file.

    -d: Search the language files for empty and duplicate translations. 
        For each duplicate set found, a list will be printed showing the various 
        translations for the same key. There is currently no option to remove duplicates
        automatically.
        
    -s: Sort the keys in all language files according to "JabRef_en.properties" and "Menu_en.properties".
        If there are duplicates or keys are missing the specific language file will be skipped!

    -t [-u]: Compare the contents of "JabRef_en.properties" and "Menu_en.properties" against the other
        language files. The program will list for all the other files which keys from the English
        file are missing. Additionally, the program will list keys in the other files which are
        not present in the English file - possible obsolete keys.
        
        If the -u option is specified, all missing keys will automatically be added to the files
        and all obsolete keys will be automatically removed.
"""

elif (len(sys.argv) >= 2) and (sys.argv[1] == "-s"):
    filesJabRef = filter(lambda s: (s.startswith('JabRef_') and not (s.startswith('JabRef_en'))), os.listdir(res_dir))
    filesJabRef = [os.path.join(res_dir, i) for i in filesJabRef]
    filesMenu = filter(lambda s: (s.startswith('Menu_') and not (s.startswith('Menu_en'))), os.listdir(res_dir))
    filesMenu = [os.path.join(res_dir, i) for i in filesMenu]

    sort_property_files_to_main_property_file(os.path.join(res_dir, "JabRef_en.properties"), filesJabRef)
    sort_property_files_to_main_property_file(os.path.join(res_dir, "Menu_en.properties"), filesMenu)

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
