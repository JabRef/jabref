# coding=utf-8

import datetime
import logging
import os
import subprocess
import sys
from io import TextIOWrapper

logging.basicConfig(level=logging.INFO)

RES_DIR = "src/main/resources/l10n"
URL_BASE = "https://github.com/JabRef/jabref/tree/master/src/main/resources/l10n/"


class Git:
    def get_current_branch(self) -> str:
        """
        :return: the current git branch
        """
        return self.__call_command('git rev-parse --abbrev-ref HEAD')

    def get_current_hash_short(self) -> str:
        """
        :return: the current git hash (short)
        """
        return self.__call_command('git rev-parse --short HEAD')

    def __call_command(self, command: str) -> str:
        """
        :param command: a shell command
        :return: the output of the shell command
        """
        return subprocess.check_output(command.split(" ")).decode("utf-8").rstrip()


class Keys:
    def __init__(self, lines):
        self.lines = lines

    def duplicates(self) -> list:
        """
        return: list of unicode strings
        """
        duplicates = []
        keys_checked = {}
        for line in self.lines:
            key, value = self.__extract_key_and_value(line=line)
            if key:
                if key in keys_checked:
                    duplicates.append("{key}={value}".format(key=key, value=value))
                    translation_in_list = "{key}={value}".format(key=key, value=keys_checked[key])
                    if translation_in_list not in duplicates:
                        duplicates.append(translation_in_list)
                else:
                    keys_checked[key] = value
        return duplicates

    def fix_duplicates(self) -> tuple:
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
                        fixed.append("{key}={value}".format(key=key, value=keys[key]))
                        keys[key] = value
                    elif not value:
                        fixed.append("{key}={value}".format(key=key, value=value))
                    elif keys[key] == value:
                        fixed.append("{key}={value}".format(key=key, value=value))
                    elif keys[key] != value:
                        not_fixed.append("{key}={value}".format(key=key, value=value))
                        not_fixed.append("{key}={value}".format(key=key, value=keys[key]))
                else:
                    keys[key] = value

        return keys, not_fixed, fixed

    def keys_from_lines(self) -> list:
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
    def key_from_line(line) -> str:
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

    def empty_keys(self) -> list:
        """
        :return: list of unicode strings: the keys with empty values
        """
        not_translated = list()
        keys = self.translations_as_dict()
        for key, value in keys.items():
            if not value:
                not_translated.append(key)
        return not_translated

    def translations_as_dict(self) -> dict:
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
    def __extract_key_and_value(line) -> tuple:
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


class SyncLang:
    def __init__(self, extended: bool, out_file='status.md'):
        """
        :param extended: boolean: if the keys with problems should be printed

        """
        self.extended = extended
        self.main_jabref_preferences = os.path.join(RES_DIR, "JabRef_en.properties")
        self.main_menu_preferences = os.path.join(RES_DIR, "Menu_en.properties")
        self.markdown_output = out_file

    def status(self):
        """
        prints the current status to the terminal
        """

        self.__print_status_jabref_properties()
        self.__print_status_menu_properties()

    def __print_status_menu_properties(self):
        self.__check_properties(main_property_file=self.main_menu_preferences, property_files=self.__all_menu_properties())

    def __print_status_jabref_properties(self):
        self.__check_properties(main_property_file=self.main_jabref_preferences, property_files=self.__all_jabref_properties())

    def update(self):
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

    def __check_properties(self, main_property_file, property_files):
        main_lines = self.__read_file_as_lines(filename=main_property_file)
        main_keys = Keys(main_lines)

        # the main property file gets compared to itself, but that is OK
        for file in property_files:
            filename = self.__format_filename(filepath=file)
            lines = self.__read_file_as_lines(file)
            keys1 = Keys(main_lines)
            keys = keys1.keys_from_lines()

            keys_missing = self.__missing_keys(main_keys.keys_from_lines(), keys)
            keys_obsolete = self.__missing_keys(keys, main_keys.keys_from_lines())
            keys_duplicate = Keys(lines).duplicates()
            keys_not_translated = Keys(lines=lines).empty_keys()

            num_keys = len(keys)
            num_keys_missing = len(keys_missing)
            num_keys_not_translated = len(keys_not_translated)
            num_keys_obsolete = len(keys_obsolete)
            num_keys_duplicate = len(keys_duplicate)
            num_keys_translated = num_keys - num_keys_not_translated

            log = logging.error if num_keys_missing != 0 or num_keys_not_translated != 0 or num_keys_obsolete != 0 or num_keys_duplicate != 0 else logging.info
            log("Status of file '{file}' with {num_keys} Keys".format(file=filename, num_keys=num_keys))
            logging.info("\t{} translated keys".format(num_keys_translated))

            log = logging.error if num_keys_not_translated != 0 else logging.info
            log("\t{} not translated keys".format(num_keys_not_translated))
            if self.extended and num_keys_not_translated != 0:
                logging.info("\t\t{}".format(", ".join(keys_not_translated)))

            log = logging.error if num_keys_missing != 0 else logging.info
            log("\t{} missing keys".format(num_keys_missing))
            if self.extended and num_keys_missing != 0:
                logging.info("\t\t{}".format(", ".join(keys_missing)))

            log = logging.error if num_keys_obsolete != 0 else logging.info
            log("\t{} obsolete keys".format(num_keys_obsolete))
            if self.extended and num_keys_obsolete != 0:
                logging.info("\t\t{}".format(", ".join(keys_obsolete)))

            log = logging.error if num_keys_duplicate != 0 else logging.info
            log("\t{} duplicates".format(num_keys_duplicate))
            if self.extended and num_keys_duplicate != 0:
                logging.info("\t\t{}".format(", ".join(keys_duplicate)))

    def __all_menu_properties(self) -> list:
        """
        :return: list of strings: all the Menu_*.preferences files with the english at the beginning
        """
        menu_property_files = sorted(self.__other_menu_properties())
        menu_property_files.insert(0, self.main_menu_preferences)
        return menu_property_files

    def __other_menu_properties(self) -> list:
        """
        :return: list of strings: all the Menu_*.preferences files without the english one
        """
        menu_property_files = [s for s in os.listdir(RES_DIR) if (s.startswith('Menu_') and not (s.startswith('Menu_en')))]
        return [os.path.join(RES_DIR, file) for file in menu_property_files]

    def __all_jabref_properties(self) -> list:
        """
        :return: list of strings: all the JabRef_*.preferences files with the english at the beginning
        """
        jabref_property_files = sorted(self.__other_jabref_properties())
        jabref_property_files.insert(0, os.path.join(RES_DIR, "JabRef_en.properties"))
        return jabref_property_files

    def __other_jabref_properties(self) -> list:
        """
        :return: list of strings: all the JabRef_*.preferences files without the english one
        """
        jabref_property_files = [s for s in os.listdir(RES_DIR) if (s.startswith('JabRef_') and not (s.startswith('JabRef_en')))]
        return [os.path.join(RES_DIR, file) for file in jabref_property_files]

    def __update_properties(self, main_property_file, other_property_files):
        main_lines = self.__read_file_as_lines(filename=main_property_file)
        main_keys = Keys(main_lines)

        main_duplicates = main_keys.duplicates()
        num_main_duplicates = len(main_duplicates)
        if num_main_duplicates != 0:
            logging.error("There are {num_duplicates} duplicates in {file}, please fix them manually".format(num_duplicates=num_main_duplicates,
                                                                                                             file=self.__format_filename(
                                                                                                                 filepath=main_property_file)))
            if self.extended:
                logging.info("\t{}".format(", ".join(main_duplicates)))
            return

        for other_property_file in other_property_files:
            filename = self.__format_filename(filepath=other_property_file)
            lines = self.__read_file_as_lines(filename=other_property_file)
            keys, not_fixed, fixed = Keys(lines).fix_duplicates()

            num_keys = len(keys)
            num_not_fixed = len(not_fixed)
            num_fixed = len(fixed)

            if num_not_fixed != 0:
                logging.error("There are {num_not_fixed_duplicates} ambiguous duplicates in {file}, please fix them manually".format(
                    num_not_fixed_duplicates=num_not_fixed, file=filename))
                if self.extended:
                    logging.error("\t{}".format(", ".join(not_fixed)))
                continue

            keys_missing = self.__missing_keys(main_keys.keys_from_lines(), keys)
            keys_obsolete = self.__missing_keys(keys, main_keys.keys_from_lines())

            num_keys_missing = len(keys_missing)
            num_keys_obsolete = len(keys_obsolete)

            for missing_key in keys_missing:
                keys[missing_key] = ""

            for obsolete_key in keys_obsolete:
                del keys[obsolete_key]

            other_lines_to_write = []
            for line in main_lines:
                key = main_keys.key_from_line(line)
                if key is not None:
                    other_lines_to_write.append("{key}={value}\n".format(key=key, value=keys[key]))
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
                if self.extended:
                    logging.info("\t\t{}".format(", ".join(fixed)))

            if num_keys_missing != 0:
                logging.info("\tadded {} missing keys".format(num_keys_missing))
                if self.extended:
                    logging.info("\t\t{}".format(", ".join(keys_missing)))

            if num_keys_obsolete != 0:
                logging.info("\tdeleted {} obsolete keys".format(num_keys_obsolete))
                if self.extended:
                    logging.info("\t\t{}".format(", ".join(keys_obsolete)))

            if sorted_lines:
                logging.info("\thas been sorted successfully")

    @staticmethod
    def __format_filename(filepath) -> str:
        """
        removes the res_dir path

        :param filepath: string
        :return: string
        """
        return filepath.replace("{}\\".format(RES_DIR), "")

    @staticmethod
    def __write_file(filename, content):
        """
        writes the lines to the file in `UTF-8`
        :param filename: string
        :param content: list of unicode unicode: the lines to write
        """
        with open(filename, 'w', newline='\n', encoding='UTF-8') as f:
            f.writelines(content)

    @staticmethod
    def __read_file_as_lines(filename, encoding="UTF-8") -> list:
        """
        :param filename: string
        :param encoding: string: the encoding of the file to read (standard: `UTF-8`)
        :return: list of unicode strings: the lines of the file
        """
        with open(filename, 'r', newline='', encoding=encoding) as file:
            return ["{}\n".format(line.strip()) for line in file.readlines()]

    def __missing_keys(self, first_list: list, second_list: list) -> list:
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

    def status_create_markdown(self):
        """
        Creates a markdown file of the current status.
        """
        def _write_properties(output_file: TextIOWrapper, property_files: list):
            output_file.write("\n| Property file | Keys | Keys translated | Keys not translated | % translated |\n")
            output_file.write("| ------------- | ---- | --------------- | ------------------- | ------------ |\n")

            for file in property_files:
                lines = self.__read_file_as_lines(file)
                keys = Keys(lines)
                num_keys = len(keys.translations_as_dict())
                num_keys_missing_value = len(keys.empty_keys())
                num_keys_translated = num_keys - num_keys_missing_value

                output_file.write(f"| [{os.path.basename(file)}]({URL_BASE}{os.path.basename(file)}) | "
                                  f"{num_keys} | "
                                  f"{num_keys_translated} | "
                                  f"{num_keys_missing_value} | "
                                  f"{_percentage(num_keys, num_keys_translated)} |\n")

        def _percentage(whole: int, part: int) -> int:
            if whole == 0:
                return 0
            return int(part / whole * 100.0)

        with open(self.markdown_output, "w", newline="\n", encoding='utf-8') as status_file:
            status_file.write(f'### Localization files status ({datetime.datetime.now().strftime("%Y-%m-%d %H:%M")} - '
                              f'Branch `{Git().get_current_branch()}` `{Git().get_current_hash_short()}`)\n\n')
            status_file.write('Note: To get the current status from your local repository, run `python ./scripts/syncLang.py markdown`\n')

            _write_properties(status_file, self.__all_menu_properties())
            _write_properties(status_file, self.__all_jabref_properties())

        logging.info(f'Current status written to {self.markdown_output}')


if '__main__' == __name__:

    if len(sys.argv) == 2 and sys.argv[1] == "markdown":
        SyncLang(extended=False, out_file='status.md').status_create_markdown()

    elif (len(sys.argv) == 2 or len(sys.argv) == 3) and sys.argv[1] == "update":
        SyncLang(extended=len(sys.argv) == 3 and (sys.argv[2] == "-e" or sys.argv[2] == "--extended")).update()

    elif (len(sys.argv) == 2 or len(sys.argv) == 3) and sys.argv[1] == "status":
        SyncLang(extended=len(sys.argv) == 3 and (sys.argv[2] == "-e" or sys.argv[2] == "--extended")).status()

    else:
        logging.info("""This program must be run from the JabRef base directory.
    
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
