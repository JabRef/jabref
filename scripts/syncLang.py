# coding=utf-8
from __future__ import print_function
import codecs
import datetime
import os
import subprocess
import sys
import webbrowser

import logger

RES_DIR = "src/main/resources/l10n"
STATUS_FILE = "status.md"
URL_BASE = "https://github.com/JabRef/jabref/tree/master/"


def get_current_branch():
    """
    :return: string: the current git branch
    """
    return subprocess.check_output(['git', 'rev-parse', '--abbrev-ref', 'HEAD']).rstrip()


def get_current_hash_short():
    """
    :return: string: the current git hash (short)
    """
    return subprocess.check_output(['git', 'rev-parse', '--short', 'HEAD']).rstrip()


def open_file(filename):
    """
    :param filename: string: opens the file with its associated application
    """
    webbrowser.open(filename)


def get_filename(filepath):
    """
    removes the res_dir path

    :param filepath: string
    :return: string
    """
    return filepath.replace("{}\\".format(RES_DIR), "")


def read_file(filename, encoding="UTF-8"):
    """
    :param filename: string
    :param encoding: string: the encoding of the file to read (standard: `UTF-8`)
    :return: list of unicode strings: the lines of the file
    """
    with codecs.open(filename, encoding=encoding) as file:
        return [u"{}\r\n".format(line.strip()) for line in file.readlines()]


def write_file(filename, content):
    """
    writes the lines to the file in `UTF-8`
    :param filename: string
    :param content: list of unicode unicode: the lines to write
    """
    codecs.open(filename, "w", encoding='utf-8').writelines(content)


def get_main_jabref_preferences():
    """
    :return: string: path to JabRef_en.preference
    """
    return os.path.join(RES_DIR, "JabRef_en.properties")


def get_other_jabref_properties():
    """
    :return: list of strings: all the JabRef_*.preferences files without the english one
    """
    jabref_property_files = filter(lambda s: (s.startswith('JabRef_') and not (s.startswith('JabRef_en'))), os.listdir(RES_DIR))
    return [os.path.join(RES_DIR, file) for file in jabref_property_files]


def get_all_jabref_properties():
    """
    :return: list of strings: all the JabRef_*.preferences files with the english at the beginning
    """
    jabref_property_files = get_other_jabref_properties()
    jabref_property_files.insert(0, os.path.join(RES_DIR, "JabRef_en.properties"))
    return jabref_property_files


def get_main_menu_properties():
    """
    :return: string: path to Menu_en.preference
    """
    return os.path.join(RES_DIR, "Menu_en.properties")


def get_other_menu_properties():
    """
    :return: list of strings: all the Menu_*.preferences files without the english one
    """
    menu_property_files = filter(lambda s: (s.startswith('Menu_') and not (s.startswith('Menu_en'))), os.listdir(RES_DIR))
    return [os.path.join(RES_DIR, file) for file in menu_property_files]


def get_all_menu_properties():
    """
    :return: list of strings: all the Menu_*.preferences files with the english at the beginning
    """
    menu_property_files = get_other_menu_properties()
    menu_property_files.insert(0, os.path.join(RES_DIR, "Menu_en.properties"))
    return menu_property_files


def get_key_from_line(line):
    """
    Tries to extract the key from the line

    :param line: unicode string
    :return: unicode string: the key or None
    """
    if line.find("#") != 0 or line.find("!") != 0:
        index_key_end = line.find("=")
        while (index_key_end > 0) and (line[index_key_end - 1] == "\\"):
            index_key_end = line.find("=", index_key_end + 1)
        if index_key_end > 0:
            return line[0:index_key_end].strip()
    return None


def get_key_and_value_from_line(line):
    """
    Tries to extract the key and value from the line

    :param line: unicode string
    :return: (unicode string, unicode string) or (None, None): (key, value)
    """
    if line.find("#") != 0 or line.find("!") != 0:
        index_key_end = line.find("=")
        while (index_key_end > 0) and (line[index_key_end - 1] == "\\"):
            index_key_end = line.find("=", index_key_end + 1)
        if index_key_end > 0:
            return line[0:index_key_end].strip(), line[index_key_end + 1:].strip()
    return None, None


def get_translations_as_dict(lines):
    """
    :param lines: list of unicode strings
    :return: dict of unicode strings:
    """
    translations = {}
    for line in lines:
        key, value = get_key_and_value_from_line(line=line)
        if key:
            translations[key] = value
    return translations


def get_empty_keys(lines):
    """
    :param lines: list of unicode strings
    :return: list of unicode strings: the keys with empty values
    """
    not_translated = []
    keys = get_translations_as_dict(lines=lines)
    for key, value in keys.iteritems():
        if not value:
            not_translated.append(key)
    return not_translated


def fix_duplicates(lines):
    """
    Fixes all unambiguous duplicates

    :param lines: list of unicode strings
    :return: (list of unicode strings, list of unicode strings): not fixed ambiguous duplicates, fixed unambiguous duplicates
    """
    keys = {}
    fixed = []
    not_fixed = []
    for line in lines:
        key, value = get_key_and_value_from_line(line=line)
        if key:
            if key in keys:
                if not keys[key]:
                    fixed.append(u"{key}={value}".format(key=key, value=keys[key]))
                    keys[key] = value
                elif not value:
                    fixed.append(u"{key}={value}".format(key=key, value=value))
                elif keys[key] == value:
                    fixed.append(u"{key}={value}".format(key=key, value=value))
                elif keys[key] != value:
                    not_fixed.append(u"{key}={value}".format(key=key, value=value))
                    not_fixed.append(u"{key}={value}".format(key=key, value=keys[key]))
            else:
                keys[key] = value

    return keys, not_fixed, fixed


def get_keys_from_lines(lines):
    """
    Builds a list of all translation keys in the list of lines.

    :param lines: a list of unicode strings
    :return: list of unicode strings: the sorted keys within the lines
    """
    keys = []
    for line in lines:
        key = get_key_from_line(line)
        if key:
            keys.append(key)
    return keys


def get_missing_keys(first_list, second_list):
    """
    Finds all keys in the first list that are not present in the second list

    :param first_list: list of unicode strings
    :param second_list: list of unicode strings
    :return: list of unicode strings
    """
    missing = []
    for key in first_list:
        if key not in second_list:
            missing.append(key)
    return missing


def get_duplicates(lines):
    """
    finds all the duplicates and returns them

    :param lines: list of unicode strings
    :return: list of unicode strings
    """
    duplicates = []
    keys_checked = {}
    for line in lines:
        key, value = get_key_and_value_from_line(line=line)
        if key:
            if key in keys_checked:
                duplicates.append(u"{key}={value}".format(key=key, value=value))
                translation_in_list = u"{key}={value}".format(key=key, value=keys_checked[key])
                if translation_in_list not in duplicates:
                    duplicates.append(translation_in_list)
            else:
                keys_checked[key] = value
    return duplicates


def status(extended):
    """
    prints the current status to the terminal

    :param extended: boolean: if the keys with problems should be printed
    """
    def check_properties(main_property_file, property_files):
        main_lines = read_file(filename=main_property_file)
        main_keys = get_keys_from_lines(lines=main_lines)

        # the main property file gets compared to itself, but that is OK
        for file in property_files:
            filename = get_filename(filepath=file)
            lines = read_file(file)
            keys = get_keys_from_lines(lines=lines)

            keys_missing = get_missing_keys(main_keys, keys)
            keys_obsolete = get_missing_keys(keys, main_keys)
            keys_duplicate = get_duplicates(lines=lines)
            keys_not_translated = get_empty_keys(lines=lines)

            num_keys = len(keys)
            num_keys_missing = len(keys_missing)
            num_keys_not_translated = len(keys_not_translated)
            num_keys_obsolete = len(keys_obsolete)
            num_keys_duplicate = len(keys_duplicate)
            num_keys_translated = num_keys - num_keys_not_translated

            log = logger.error if num_keys_missing != 0 or num_keys_not_translated != 0 or num_keys_obsolete != 0 or num_keys_duplicate != 0 else logger.ok
            log("Status of file '{file}' with {num_keys} Keys".format(file=filename, num_keys=num_keys))
            logger.ok("\t{} translated keys".format(num_keys_translated))

            log = logger.error if num_keys_not_translated != 0 else logger.ok
            log("\t{} not translated keys".format(num_keys_not_translated))
            if extended and num_keys_not_translated != 0:
                logger.neutral(u"\t\t{}".format(", ".join(keys_not_translated)))

            log = logger.error if num_keys_missing != 0 else logger.ok
            log("\t{} missing keys".format(num_keys_missing))
            if extended and num_keys_missing != 0:
                logger.neutral(u"\t\t{}".format(", ".join(keys_missing)))

            log = logger.error if num_keys_obsolete != 0 else logger.ok
            log("\t{} obsolete keys".format(num_keys_obsolete))
            if extended and num_keys_obsolete != 0:
                logger.neutral(u"\t\t{}".format(", ".join(keys_obsolete)))

            log = logger.error if num_keys_duplicate != 0 else logger.ok
            log("\t{} duplicates".format(num_keys_duplicate))
            if extended and num_keys_duplicate != 0:
                logger.neutral(u"\t\t{}".format(", ".join(keys_duplicate)))

    check_properties(main_property_file=get_main_jabref_preferences(), property_files=get_all_jabref_properties())
    check_properties(main_property_file=get_main_menu_properties(), property_files=get_all_menu_properties())


def update(extended):
    """
    updates all the localization files
    fixing unambiguous duplicates, removing obsolete keys, adding missing keys, and sorting them

    :param extended: boolean: if the keys with problems should be printed
    """
    def update_properties(main_property_file, other_property_files):
        main_lines = read_file(filename=main_property_file)
        # saved the stripped lines
        write_file(main_property_file, main_lines)
        main_keys = get_keys_from_lines(lines=main_lines)

        main_duplicates = get_duplicates(lines=main_lines)
        num_main_duplicates = len(main_duplicates)
        if num_main_duplicates != 0:
            logger.error("There are {num_duplicates} duplicates in {file}, please fix them manually".format(num_duplicates=num_main_duplicates, file=get_filename(filepath=main_property_file)))
            if extended:
                logger.neutral(u"\t{}".format(", ".join(main_duplicates)))
            return


        for other_property_file in other_property_files:
            filename = get_filename(filepath=other_property_file)
            lines = read_file(filename=other_property_file)
            keys, not_fixed, fixed = fix_duplicates(lines=lines)

            num_keys = len(keys)
            num_not_fixed = len(not_fixed)
            num_fixed = len(fixed)

            if num_not_fixed != 0:
                logger.error("There are {num_not_fixed_duplicates} ambiguous duplicates in {file}, please fix them manually".format(num_not_fixed_duplicates=num_not_fixed, file=filename))
                if extended:
                    logger.error(u"\t{}".format(u", ".join(not_fixed)))
                continue

            keys_missing = get_missing_keys(main_keys, keys)
            keys_obsolete = get_missing_keys(keys, main_keys)

            num_keys_missing = len(keys_missing)
            num_keys_obsolete = len(keys_obsolete)

            for missing_key in keys_missing:
                keys[missing_key] = ""

            for obsolete_key in keys_obsolete:
                del keys[obsolete_key]

            other_lines_to_write = []
            for line in main_lines:
                key = get_key_from_line(line)
                if key is not None:
                    other_lines_to_write.append(u"{key}={value}\r\n".format(key=key, value=keys[key]))
                else:
                    other_lines_to_write.append(line)

            sorted = len(lines) != len(other_lines_to_write)
            if not sorted:
                for old_line, new_lines in zip(lines, other_lines_to_write):
                    if old_line != new_lines:
                        sorted = True

            write_file(filename=other_property_file, content=other_lines_to_write)

            logger.ok("Processing file '{file}' with {num_keys} Keys".format(file=filename, num_keys=num_keys))
            if num_fixed != 0:
                logger.ok("\tfixed {} unambiguous duplicates".format(num_fixed))
                if extended:
                    logger.neutral(u"\t\t{}".format(", ".join(fixed)))

            if num_keys_missing != 0:
                logger.ok("\tadded {} missing keys".format(num_keys_missing))
                if extended:
                    logger.neutral(u"\t\t{}".format(", ".join(keys_missing)))

            if num_keys_obsolete != 0:
                logger.ok("\tdeleted {} obsolete keys".format(num_keys_obsolete))
                if extended:
                    logger.neutral(u"\t\t{}".format(", ".join(keys_obsolete)))

            if sorted:
                logger.ok("\thas been sorted successfully")

    update_properties(main_property_file=get_main_jabref_preferences(), other_property_files=get_other_jabref_properties())
    update_properties(main_property_file=get_main_menu_properties(), other_property_files=get_other_menu_properties())


def status_create_markdown():
    """
    creates a markdown file of the current status and opens it
    """
    def write_properties(property_files):
        markdown.append("\n| Property file | Keys | Keys translated | Keys not translated | % translated |\n")
        markdown.append("| ------------- | ---- | --------------- | ------------------- | ------------ |\n")

        for file in property_files:
            lines = read_file(file)
            keys = get_translations_as_dict(lines=lines)
            keys_missing_value = get_empty_keys(lines=lines)

            num_keys = len(keys)
            num_keys_missing_value = len(keys_missing_value)
            num_keys_translated = num_keys - num_keys_missing_value
            percent_translated = int((num_keys_translated / float(num_keys)) * 100) if num_keys != 0 else 0

            markdown.append("| [{file}]({url_base}{file}) | {num_keys} | {num_keys_translated} | {num_keys_missing} | {percent_translated} |\n"
         .format(url_base=URL_BASE, file=os.path.basename(file), num_keys=num_keys, num_keys_translated=num_keys_translated, num_keys_missing=num_keys_missing_value, percent_translated=percent_translated))

    markdown = []
    date = datetime.datetime.now().strftime("%Y-%m-%d %H:%M")
    markdown.append("### Localization files status ({date} - Branch `{branch}` `{hash}`)\n".format(date=date, branch=get_current_branch(), hash=get_current_hash_short()))

    write_properties(property_files=get_all_jabref_properties())
    write_properties(property_files=get_all_menu_properties())
    write_file(STATUS_FILE, markdown)
    logger.ok("Current status written to {}".format(STATUS_FILE))
    open_file(STATUS_FILE)


if len(sys.argv) == 2 and sys.argv[1] == "markdown":
    status_create_markdown()

elif (len(sys.argv) == 2 or len(sys.argv) == 3) and sys.argv[1] == "update":
    update(extended=len(sys.argv) == 3 and (sys.argv[2] == "-e" or sys.argv[2] == "--extended"))

elif (len(sys.argv) == 2 or len(sys.argv) == 3) and sys.argv[1] == "status":
    status(extended=len(sys.argv) == 3 and (sys.argv[2] == "-e" or sys.argv[2] == "--extended"))

else:
    logger.neutral("""This program must be run from the JabRef base directory.

Usage: syncLang.py {markdown, status [-e | --extended], update [-e | --extended]}
Option can be one of the following:

    status [-e | --extended]:
        prints the current status to the terminal
        [-e | --extended]:
            if the translations keys which create problems should be printed

    markdown:
        Creates a markdown file of the current status and opens it

    update [-e | --extended]:
        compares all the localization files against the English one and fixes unambiguous duplicates,
        removes obsolete keys, adds missing keys, and sorts them
        [-e | --extended]:
            if the translations keys which create problems should be printed
""")
