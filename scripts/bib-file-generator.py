"""Create a large BibTeX library for startup and group-matching profiling."""

from pathlib import Path


NUMBER_OF_ENTRIES = 100_000
OUTPUT_FILE = Path("generated-large-library.bib")
KEYWORD_SEPARATOR = ","

TOP_LEVEL_KEYWORDS = [
    "Topic 00",
    "Topic 01",
    "Topic 02",
    "Topic 03",
    "Topic 04",
    "Topic 05",
    "Topic 06",
    "Topic 07",
    "Topic 08",
    "Topic 09",
]

SUB_KEYWORDS_PER_TOPIC = 10
FLAGS = [
    "Flag 00",
    "Flag 01",
    "Flag 02",
    "Flag 03",
    "Flag 04",
]


# Adapted from: https://stackoverflow.com/a/50012689/873282
def int_to_roman(num):
    values = [
        1000000, 900000, 500000, 400000, 100000, 90000, 50000, 40000, 10000, 9000, 5000, 4000, 1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1
    ]
    strings = [
        "M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"
    ]

    result = ""
    decimal = num

    while decimal > 0:
        for index, value in enumerate(values):
            if decimal >= value:
                if value > 1000:
                    result += "\u0304".join(list(strings[index])) + "\u0304"
                else:
                    result += strings[index]
                decimal -= value
                break
    return result


def escape_group_value(value):
    return value.replace("\\", "\\\\").replace(";", "\\;")


def group_tree_metadata():
    lines = [
        "@comment{jabref-meta: saveOrderConfig:specified;author;false;title;false;year;false;}",
        "",
        "@comment{jabref-meta: groupsversion:3;}",
        "",
        "@comment{jabref-meta: groupstree:",
        "0 AllEntriesGroup:;",
    ]

    for topic in TOP_LEVEL_KEYWORDS:
        lines.append(
            f"1 KeywordGroup:{escape_group_value(topic)}\\;0\\;keywords\\;{escape_group_value(topic)}\\;0\\;0\\;;"
        )

        for sub_index in range(SUB_KEYWORDS_PER_TOPIC):
            sub_keyword = f"{topic} / Subtopic {sub_index:02d}"
            lines.append(
                f"2 KeywordGroup:{escape_group_value(sub_keyword)}\\;0\\;keywords\\;{escape_group_value(sub_keyword)}\\;0\\;0\\;;"
            )

    for flag in FLAGS:
        lines.append(
            f"1 KeywordGroup:{escape_group_value(flag)}\\;0\\;keywords\\;{escape_group_value(flag)}\\;0\\;0\\;;"
        )

    lines.extend(["}", ""])
    return "\n".join(lines)


def keywords_for_entry(entry_number):
    topic_index = (entry_number - 1) % len(TOP_LEVEL_KEYWORDS)
    subtopic_index = ((entry_number - 1) // len(TOP_LEVEL_KEYWORDS)) % SUB_KEYWORDS_PER_TOPIC
    flag_index = ((entry_number - 1) // (len(TOP_LEVEL_KEYWORDS) * SUB_KEYWORDS_PER_TOPIC)) % len(FLAGS)

    topic = TOP_LEVEL_KEYWORDS[topic_index]
    subtopic = f"{topic} / Subtopic {subtopic_index:02d}"
    flag = FLAGS[flag_index]

    return KEYWORD_SEPARATOR.join((topic, subtopic, flag))


def entry_text(entry_number):
    year = 1900 + (entry_number - 1) % (2025 - 1900)
    keywords = keywords_for_entry(entry_number)
    return f"""@article{{id{entry_number:06d},
  title    = {{This is my title{entry_number}}},
  author   = {{FirstnameA{entry_number} LastnameA{entry_number} and FirstnameB{entry_number} LastnameB{entry_number} and FirstnameC{entry_number} LastnameC{entry_number}}},
  journal  = {{Journal Title {int_to_roman(entry_number)}}},
  volume   = {{{entry_number}}},
  year     = {{{year}}},
  keywords = {{{keywords}}},
}}

"""


with OUTPUT_FILE.open("w", encoding="utf-8") as file:
    file.write(group_tree_metadata())
    for entry_number in range(1, NUMBER_OF_ENTRIES + 1):
        file.write(entry_text(entry_number))
