# coding=utf-8
from __future__ import division

import argparse  # Requires Python 2.7, should be safe to use with jython.
import codecs
import datetime
import logging
import os
import subprocess
import sys

logging.basicConfig(level=logging.INFO)

RES_DIR = "src/main/resources/l10n"
URL_BASE = "https://github.com/JabRef/jabref/tree/master/src/main/resources/l10n/"

try:
    # Just to make sure not to break anything, in case py3.x is not supported.
    import pathlib

    class PathFinder:
        """
        This class is designed to automatically locate this script's path within the repository.
        Once it found it's location it can easily provide paths to other important directories and files.
        
        requires Python 3.4 or higher
        """
        
        BASE_DIRECTORY_NAME = 'jabref'
        
        @staticmethod
        def getJabRefBaseDirectory():
            """
            Searches the script's path backwards until it finds the matching base directory.
            :return the path to JabRef's base directory as pathlib.Path object.
            """
            cwd = pathlib.Path.cwd()
            if cwd.name == PathFinder.BASE_DIRECTORY_NAME:
                return cwd
            
            for parent in cwd.parents:
                if parent.name == PathFinder.BASE_DIRECTORY_NAME:
                    return parent
            # TODO What to do if base directory could not be found?
    
    # Important directories of the JabRef repository
    JABREF_BASE_DIRECTORY = PathFinder.getJabRefBaseDirectory()
    JABREF_SOURCE_DIRECTORY = JABREF_BASE_DIRECTORY / 'src'
    JABREF_SCRIPTS_DIRECTORY = JABREF_BASE_DIRECTORY / 'scripts'
    JABREF_LOCALIZATION_DIRECTORY = JABREF_SOURCE_DIRECTORY / 'main/resources/l10n'
    
    # Important files
    JABREF_MAIN_LOCALIZATION_FILE = JABREF_LOCALIZATION_DIRECTORY / 'JabRef_en.properties'
    JABREF_MAIN_MENU_LOCALIZATION_FILE = JABREF_LOCALIZATION_DIRECTORY / 'Menu_en.properties'
except ImportError:
    logging.info("Unable to use PathFinder class.")

    
class Git:

    def get_current_branch(self):
        """
        :return: the current git branch
        """
        return self.__call_command('git rev-parse --abbrev-ref HEAD')

    def get_current_hash_short(self):
        """
        :return: the current git hash (short)
        """
        return self.__call_command('git rev-parse --short HEAD')

    def __call_command(self, command):
        """
        :param command: a shell command
        :return: the output of the shell command
        """
        return subprocess.check_output(command.split(" ")).decode("utf-8").rstrip()


class Keys:

    def __init__(self, lines):
        self.lines = lines

    def find_duplicates(self):
        """
        return: list of unicode strings
        """
        duplicates = []
        keys_checked = {}
        for line in self.lines:
            key, value = self.__extract_key_and_value(line=line)
            if key:
                if key in keys_checked:
                    duplicates.append(self.format_key_and_value(key=key, value=value))
                    translation_in_list = self.format_key_and_value(key=key, value=keys_checked[key])
                    if translation_in_list not in duplicates:
                        duplicates.append(translation_in_list)
                else:
                    keys_checked[key] = value
        return duplicates

    def fix_duplicates(self):
        """
        Fixes all unambiguous duplicates
        :return: (list of unicode strings, list of unicode strings): not fixed ambiguous duplicates, fixed unambiguous duplicates
        """
        keys = {}
        fixed = []
        not_fixed = []
        for line in self.lines:
            key, value = self.__extract_key_and_value(line=line)
            if key:
                if key in keys:
                    if not keys[key]:
                        fixed.append(self.format_key_and_value(key=key, value=keys[key]))
                        keys[key] = value
                    elif not value:
                        fixed.append(self.format_key_and_value(key=key, value=value))
                    elif keys[key] == value:
                        fixed.append(self.format_key_and_value(key=key, value=value))
                    elif keys[key] != value:
                        not_fixed.append(self.format_key_and_value(key=key, value=value))
                        not_fixed.append(self.format_key_and_value(key=key, value=keys[key]))
                else:
                    keys[key] = value

        return keys, not_fixed, fixed

    def keys_from_lines(self):
        """
        Builds a list of all translation keys in the list of lines.

        :return: list of unicode strings: the sorted keys within the lines
        """
        keys = list()
        for line in self.lines:
            key = self.key_from_line(line)
            if key:
                keys.append(key)
        return keys

    @staticmethod
    def key_from_line(line):
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

    def empty_keys(self):
        """
        :return: list of unicode strings: the keys with empty values
        """
        not_translated = list()
        keys = self.translations_as_dict()
        for key, value in keys.items():
            if not value:
                not_translated.append(key)
        return not_translated

    def translations_as_dict(self):
        """
        :return: dict of unicode strings:
        """
        translations = dict()
        for line in self.lines:
            key, value = self.__extract_key_and_value(line=line)
            if key:
                translations[key] = value
        return translations

    @staticmethod
    def __extract_key_and_value(line):
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
    
    @staticmethod
    def format_key_and_value(key, value):
        return u"{key}={value}".format(key=key, value=value)


class SyncLang:

    def __init__(self, extended_logging=False):
        """
        :param extended: boolean: if the keys with problems should be printed

        """
        self.extended_logging = extended_logging
        self.main_jabref_preferences = os.path.join(RES_DIR, "JabRef_en.properties")
        self.main_menu_preferences = os.path.join(RES_DIR, "Menu_en.properties")
        
    def set_extended_logging_enabled(self, value):
        self.extended_logging = bool(value)
        
    def print_missing_keys(self):
        for file in self.__other_jabref_properties():
            file_name = self.__format_filename(file)
            self.print_missing_keys_for_file(file_name)
        for file in self.__other_menu_properties():
            file_name = self.__format_filename(file)
            self.print_missing_keys_for_file(file_name)
        
    def print_missing_keys_for_file(self, file_name):
        file_status = self.__get_status_for_file(file_name)
        if file_status:
            keys_missing, _, _, _ = file_status
            
            if len(keys_missing) > 0:
                logging.info("Printing missing keys for file: " + file_name)
                self.__print_keys(keys_missing)
            else:
                logging.info("No missing keys found for file:" + file_name)
                
    def print_obsolete_keys(self):
        for file in self.__other_jabref_properties():
            file_name = self.__format_filename(file)
            self.print_obsolete_keys_for_file(file_name)
        for file in self.__other_menu_properties():
            file_name = self.__format_filename(file)
            self.print_obsolete_keys_for_file(file_name)
                
    
    def print_obsolete_keys_for_file(self, file_name):
        file_status = self.__get_status_for_file(file_name)
        if file_status:
            _, keys_obsolete, _, _ = file_status
            
            if len(keys_obsolete) > 0:
                
                logging.info("Printing obsolete keys for file: " + file_name)
                self.__print_keys(keys_obsolete)
            else:
                logging.info("No obsolete keys found for file: " + file_name)
                
    def print_duplicate_keys(self):
        for file in self.__other_jabref_properties():
            file_name = self.__format_filename(file)
            self.print_duplicate_keys_for_file(file_name)
        for file in self.__other_menu_properties():
            file_name = self.__format_filename(file)
            self.print_duplicate_keys_for_file(file_name)

    def print_duplicate_keys_for_file(self, file_name):
        file_status = self.__get_status_for_file(file_name)
        if file_status:
            _, _, keys_duplicate, _ = file_status
            
            if len(keys_duplicate) > 0:
                
                logging.info("Printing duplicate keys for file: " + file_name)
                self.__print_keys(keys_duplicate)
            else:
                logging.info("No duplicate keys found for file: " + file_name)

    def status(self):
        """
        prints the current status to the terminal
        """

        self.__print_status_jabref_properties()
        self.__print_status_menu_properties()

    def __print_status_menu_properties(self):
        self.__compare_properties(main_property_file=self.main_menu_preferences, property_files=self.__all_menu_properties())

    def __print_status_jabref_properties(self):
        self.__compare_properties(main_property_file=self.main_jabref_preferences, property_files=self.__all_jabref_properties())

    def update_properties(self):
        """
        updates all the localization files
        fixing unambiguous duplicates, removing obsolete keys, adding missing keys, and sorting them
        """

        self.__update_jabref_properties()
        self.__update_menu_properties()

    def __update_menu_properties(self):
        self.__update_properties(main_property_file=self.main_menu_preferences, other_property_files=self.__other_menu_properties())

    def __update_jabref_properties(self):
        self.__update_properties(main_property_file=self.main_jabref_preferences, other_property_files=self.__other_jabref_properties())
        
    def __get_main_file(self, file_name):
        
        file = os.path.join(RES_DIR, file_name)
        
        if file in self.__all_jabref_properties():
            return self.main_jabref_preferences
        elif file in self.__all_menu_properties():
            return self.main_menu_preferences
        
    def __get_status_for_file(self, file_name):
        main_file = self.__get_main_file(file_name)
        if main_file:
            main_lines = self.__read_lines_from_file(filename=main_file)
            main_keys = Keys(main_lines)
            
            file = os.path.join(RES_DIR, file_name)
            lines = self.__read_lines_from_file(file)
            keys1 = Keys(lines)
            keys = keys1.keys_from_lines()

            keys_missing = self.__missing_keys(main_keys.keys_from_lines(), keys)
            keys_obsolete = self.__missing_keys(keys, main_keys.keys_from_lines())
            keys_duplicates = keys1.find_duplicates()
            keys_not_translated = keys1.empty_keys()
            
            return (keys_missing, keys_obsolete, keys_duplicates, keys_not_translated)
        else:
            logging.debug("Unable to find main file for: " + file_name)

    def __compare_properties(self, main_property_file, property_files):
        main_lines = self.__read_lines_from_file(filename=main_property_file)
        main_keys = Keys(main_lines)

        # the main property file gets compared to itself, but that is OK
        for file in property_files:
            filename = self.__format_filename(filepath=file)
            lines = self.__read_lines_from_file(file)
            keys1 = Keys(lines)
            keys = keys1.keys_from_lines()

            keys_missing = self.__missing_keys(main_keys.keys_from_lines(), keys)
            keys_obsolete = self.__missing_keys(keys, main_keys.keys_from_lines())
            keys_duplicates = keys1.find_duplicates()
            keys_not_translated = keys1.empty_keys()

            num_keys = len(keys)
            num_keys_missing = len(keys_missing)
            num_keys_not_translated = len(keys_not_translated)
            num_keys_obsolete = len(keys_obsolete)
            num_keys_duplicate = len(keys_duplicates)
            num_keys_translated = num_keys - num_keys_not_translated

            log = logging.error if num_keys_missing != 0 or num_keys_not_translated != 0 or num_keys_obsolete != 0 or num_keys_duplicate != 0 else logging.info
            log("Status of file '{file}' with {num_keys} Keys".format(file=filename, num_keys=num_keys))
            logging.info("\t{} translated keys".format(num_keys_translated))

            log = logging.error if num_keys_not_translated != 0 else logging.info
            log("\t{} not translated keys".format(num_keys_not_translated))
            if self.extended_logging and num_keys_not_translated != 0:
                self.__print_keys(keys_not_translated)

            log = logging.error if num_keys_missing != 0 else logging.info
            log("\t{} missing keys".format(num_keys_missing))
            if self.extended_logging and num_keys_missing != 0:
                self.__print_keys(keys_missing)

            log = logging.error if num_keys_obsolete != 0 else logging.info
            log("\t{} obsolete keys".format(num_keys_obsolete))
            if self.extended_logging and num_keys_obsolete != 0:
                self.__print_keys(keysobsolete)

            log = logging.error if num_keys_duplicate != 0 else logging.info
            log("\t{} duplicates".format(num_keys_duplicate))
            if self.extended_logging and num_keys_duplicate != 0:
                self.__print_keys(keys_duplicates)

    def __all_menu_properties(self):
        """
        :return: list of strings: all the Menu_*.preferences files with the english at the beginning
        """
        menu_property_files = sorted(self.__other_menu_properties())
        menu_property_files.insert(0, self.main_menu_preferences)
        return menu_property_files

    def __other_menu_properties(self):
        """
        :return: list of strings: all the Menu_*.preferences files without the english one
        """
        menu_property_files = [s for s in os.listdir(RES_DIR) if (s.startswith('Menu_') and not (s.startswith('Menu_en')))]
        return [os.path.join(RES_DIR, file) for file in menu_property_files]

    def __all_jabref_properties(self):
        """
        :return: list of strings: all the JabRef_*.preferences file paths with the english at the beginning
        """
        jabref_property_files = sorted(self.__other_jabref_properties())
        jabref_property_files.insert(0, self.main_jabref_preferences)
        return jabref_property_files

    def __other_jabref_properties(self):
        """
        :return: list of strings: all the JabRef_*.preferences file paths without the english one
        """
        jabref_property_files = [s for s in os.listdir(RES_DIR) if (s.startswith('JabRef_') and not (s.startswith('JabRef_en')))]
        return [os.path.join(RES_DIR, file) for file in jabref_property_files]

    def __update_properties(self, main_property_file, other_property_files):
        main_lines = self.__read_lines_from_file(filename=main_property_file)
        main_keys = Keys(main_lines)
        main_keys_dict = main_keys.translations_as_dict()

        main_duplicates = main_keys.find_duplicates()
        num_main_duplicates = len(main_duplicates)
        if num_main_duplicates != 0:
            logging.error("There are {num_duplicates} duplicates in {file}, please fix them manually".format(num_duplicates=num_main_duplicates,
                                                                                                             file=self.__format_filename(
                                                                                                                 filepath=main_property_file)))
            if self.extended_logging:
                self.__print_keys(main_duplicates)
            return

        for other_property_file in other_property_files:
            filename = self.__format_filename(filepath=other_property_file)
            lines = self.__read_lines_from_file(filename=other_property_file)
            keys, not_fixed, fixed = Keys(lines).fix_duplicates()

            num_keys = len(keys)
            num_not_fixed = len(not_fixed)
            num_fixed = len(fixed)

            if num_not_fixed != 0:
                logging.error("There are {num_not_fixed_duplicates} ambiguous duplicates in {file}, please fix them manually".format(
                    num_not_fixed_duplicates=num_not_fixed, file=filename))
                if self.extended_logging:
                    self.__print_keys(not_fixed)
                continue

            keys_missing = self.__missing_keys(main_keys.keys_from_lines(), keys)
            keys_obsolete = self.__missing_keys(keys, main_keys.keys_from_lines())

            num_keys_missing = len(keys_missing)
            num_keys_obsolete = len(keys_obsolete)

            # for missing_key in keys_missing:
                # Missing keys are added with main translation by default.
                # keys[missing_key] = main_keys_dict[missing_key]

            for obsolete_key in keys_obsolete:
                del keys[obsolete_key]

            other_lines_to_write = []
            for line in main_lines:
                key = main_keys.key_from_line(line)
                if key is not None:
                    if keys.has_key(key):
                        # Do not write empty keys
                        if keys[key] != "":
                            other_lines_to_write.append(Keys.format_key_and_value(key=key, value=keys[key]) + "\n")
                else:
                    other_lines_to_write.append(line)

            sorted_lines = len(lines) != len(other_lines_to_write)
            if not sorted_lines:
                for old_line, new_lines in zip(lines, other_lines_to_write):
                    if old_line != new_lines:
                        sorted_lines = True

            self.__write_file(filename=other_property_file, content=other_lines_to_write)

            logging.info("Processing file '{file}' with {num_keys} Keys".format(file=filename, num_keys=num_keys))
            if num_fixed != 0:
                logging.info("\tfixed {} unambiguous duplicates".format(num_fixed))
                if self.extended_logging:
                    self.__print_keys(fixed)

            if num_keys_missing != 0:
                logging.info("\tadded {} missing keys".format(num_keys_missing))
                if self.extended_logging:
                    self.__print_keys(keys_missing)

            if num_keys_obsolete != 0:
                logging.info("\tdeleted {} obsolete keys".format(num_keys_obsolete))
                if self.extended_logging:
                    self.__print_keys(keys_obsolete)

            if sorted_lines:
                logging.info("\thas been sorted successfully")

    @staticmethod
    def __format_filename(filepath):
        """
        removes the res_dir path

        :param filepath: string
        :return: pure file name of this file path (including file extension e.g. *.txt)
        """
        return filepath.replace("{}\\".format(RES_DIR), "")

    @staticmethod
    def __write_file(filename, content):
        """
        writes the lines to the file in `UTF-8`
        :param filename: string
        :param content: list of unicode strings: the lines to write
        """
        with codecs.open(filename, 'w', encoding="UTF-8") as f:
            f.writelines(content)

    @staticmethod
    def __read_lines_from_file(filename):
        """
        :param filename: string
        :param encoding: string: the encoding of the file to read (standard: `UTF-8`)
        :return: list of unicode strings: the lines of the file
        """
        with codecs.open(filename, 'r', encoding="UTF-8") as file:
            return [u"{}\n".format(line.strip()) for line in file.readlines()]

    @staticmethod
    def __missing_keys(first_list, second_list):
        """
        Finds all keys in the first list that are not present in the second list

        :param first_list: list of unicode strings
        :param second_list: list of unicode strings
        :return: list of unicode strings
        """
        return list(set(first_list).difference(second_list))
    
    @staticmethod
    def __print_keys(keys, logger=logging.info):
        for key in keys:
            logger("\t{}\n".format(key))

    def status_create_markdown(self, markdown_file='status.md'):
        """
        Creates a markdown file of the current status.
        """

        def _write_properties(output_file, property_files):
            output_file.write("\n| Property file | Keys | Keys translated | Keys not translated | % translated |\n")
            output_file.write("| ------------- | ---- | --------------- | ------------------- | ------------ |\n")

            for file in property_files:
                lines = self.__read_lines_from_file(file)
                keys = Keys(lines)
                num_keys = len(keys.translations_as_dict())
                num_keys_missing_value = len(keys.empty_keys())
                num_keys_translated = num_keys - num_keys_missing_value

                output_file.write("| [%s](%s%s) | %d | %d | %d | %d |\n" % (os.path.basename(file),
                                                                            URL_BASE,
                                                                            os.path.basename(file),
                                                                            num_keys,
                                                                            num_keys_translated,
                                                                            num_keys_missing_value,
                                                                            _percentage(num_keys, num_keys_translated)
                                                                            )
                                  )

        def _percentage(whole, part):
            if whole == 0:
                return 0
            return int(part / whole * 100.0)

        with codecs.open(markdown_file, "w", encoding="UTF-8") as status_file:
            status_file.write('### Localization files status (' + datetime.datetime.now().strftime(
                "%Y-%m-%d %H:%M") + ' - Branch `' + Git().get_current_branch() + '` `' + Git().get_current_hash_short() + '`)\n\n')
            status_file.write('Note: To get the current status from your local repository, run `python ./scripts/syncLang.py markdown`\n')

            _write_properties(status_file, self.__all_menu_properties())
            _write_properties(status_file, self.__all_jabref_properties())

        logging.info('Current status written to ' + markdown_file)


def main():
    
    syncer = SyncLang()
    
    def markdown_command(args):
        syncer.set_extended_logging_enabled(False)
        syncer.status_create_markdown()

    def status_command(args):
        syncer.set_extended_logging_enabled(args.extended)
        syncer.status()

    def update_command(args):
        syncer.set_extended_logging_enabled(args.extended)
        syncer.update_properties()

    def print_missing(args):
        file_name = args.file
        if file_name:
            syncer.print_missing_keys_for_file(file_name)
        else:
            syncer.print_missing_keys()

    def print_obsolete(args):
        file_name = args.file
        if file_name:
            syncer.print_obsolete_keys_for_file(file_name)
        else:
            syncer.print_obsolete_keys()

    def print_duplicates(args):
        file_name = args.file
        if file_name:
            syncer.print_duplicate_keys_for_file(file_name)
        else:
            syncer.print_duplicate_keys()
    
    parser = argparse.ArgumentParser(add_help=True)
    parser.description = "This script is used to synchronize the keys of different *.properties files."
    
    shared_arguments = argparse.ArgumentParser(add_help=False)
    extended_argument = shared_arguments.add_argument("-e", "--extended", help="Prints extended information about the process to the terminal", required=False, action='store_true', default=False)
    
    subcommands = parser.add_subparsers(title="Subcommands", description="Provide different options for the user", dest="subcommand")
    
    # markdown parser
    markdown_parser = subcommands.add_parser("markdown", description="Creates a markdown file of the current status")
    markdown_parser.set_defaults(func=markdown_command)
    # TODO add argument to pass a file name for the markdown file
    
    # status parser
    status_parser = subcommands.add_parser("status", description="Prints the current status to the terminal", parents=[shared_arguments])
    status_parser.set_defaults(func=status_command)
    
    # update parser
    update_parser = subcommands.add_parser("update", description="Compares all the localization files against the English one and fixes unambiguous duplicates, removes obsolete keys, adds missing keys, and sorts them", parents=[shared_arguments])
    update_parser.set_defaults(func=update_command)
    
    # print parser
    print_parser = subcommands.add_parser("print", description="Prints specific status info to the console")
    
    shared_print_arguments = argparse.ArgumentParser(add_help=False)
    file_argument = shared_print_arguments.add_argument("-f", "--file", help="Specifies a file for the command to run with", required=False, action='store')
    
    print_options = print_parser.add_subparsers(title="Print Options", description="Different options for printing", dest="print_option_name")
    
    missing_parser = print_options.add_parser("missing", description="Prints all missing keys", parents=[shared_print_arguments])
    missing_parser.set_defaults(func=print_missing)
    
    obsolete_parser = print_options.add_parser("obsolete", description="Prints all obsolete keys", parents=[shared_print_arguments])
    obsolete_parser.set_defaults(func=print_obsolete)
    
    duplicates_parser = print_options.add_parser("duplicates", description="Prints all duplicate keys", parents=[shared_print_arguments])
    duplicates_parser.set_defaults(func=print_duplicates)
    
    parsed_args = parser.parse_args()
    parsed_args.func(parsed_args)


if '__main__' == __name__:
    main()
