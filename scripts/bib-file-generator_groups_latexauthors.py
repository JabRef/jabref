"""Create a large BibTeX library for startup and group-matching profiling."""

from pathlib import Path


NUMBER_OF_ENTRIES = 100_000
OUTPUT_FILE = Path("generated-large-library.bib")
KEYWORD_SEPARATOR = ","
KEYWORD_HIERARCHY_SEPARATOR = ">"

TOPICS = {
    "Machine learning": [
        "Representation learning",
        "Graph neural networks",
        "Probabilistic models",
        "Reinforcement learning",
    ],
    "Human-computer interaction": [
        "Accessibility",
        "Visualization",
        "Collaboration tools",
        "User studies",
    ],
    "Software engineering": [
        "Testing",
        "Static analysis",
        "Program comprehension",
        "Build systems",
    ],
    "Digital libraries": [
        "Metadata quality",
        "Deduplication",
        "Scholarly search",
        "Citation analysis",
    ],
    "Information retrieval": [
        "Ranking",
        "Query expansion",
        "Entity linking",
        "Evaluation",
    ],
    "Data management": [
        "Data integration",
        "Knowledge graphs",
        "Stream processing",
        "Reproducibility",
    ],
}

METHODS = [
    "Systematic review",
    "Controlled experiment",
    "Benchmark study",
    "Case study",
    "Simulation",
    "Survey",
]

READ_STATUS = [
    "unread",
    "skimmed",
    "read",
]

VENUES = [
    "Journal of Open Research Software",
    "Empirical Software Engineering",
    "Information Processing and Management",
    "ACM Transactions on Information Systems",
    "Scientometrics",
    "Journal of Documentation",
    "Research Evaluation",
    "International Journal on Digital Libraries",
    "SoftwareX",
    "Data and Knowledge Engineering",
]

TITLE_PATTERNS = [
    "A comparative study of {subtopic} for {topic_lower}",
    "Improving {topic_lower} with {subtopic_lower}",
    "An empirical evaluation of {subtopic_lower} in {topic_lower}",
    "Towards reproducible {topic_lower}: lessons from {subtopic_lower}",
    "{subtopic} in practice: evidence from {method_lower}",
]

FIRST_NAMES = [
    "Ada", "Alan", "Alice", "Amelia", "Anders", "Anika", "Beatrice", "Benedikt", "Bjorn", "Carla",
    "Carlos", "Celine", "Clara", "Daniel", "Daphne", "David", "Elena", "Elias", "Elise", "Emma",
    "Felix", "Franziska", "George", "Grace", "Greta", "Hannah", "Hugo", "Ida", "Iris", "Jakob",
    "Jana", "Jonas", "Julia", "Kai", "Karla", "Lara", "Laura", "Lea", "Leon", "Lina",
    "Linus", "Lotte", "Louis", "Luca", "Lukas", "Maja", "Mara", "Marie", "Matteo", "Max",
    "Mia", "Mila", "Mira", "Moritz", "Nina", "Noah", "Nora", "Oskar", "Paula", "Paul",
    "Quentin", "Rafael", "Robert", "Rosa", "Sofia", "Sophie", "Theo", "Tobias", "Valentina", "Yara",
    "Bj{\\\"o}rn", "J{\\\"u}rgen", "M{\\\"a}rta", "G{\\\"u}nther", "S{\\\"o}ren", "L{\\\"u}ne",
    "{\\L}ukasz", "Ma{\\l}gorzata", "{\\Z}aneta", "Bartosz", "Joanna", "{\\S}wiatos{\\l}aw", "{\\'E}lodie", "Ji{\\v r}{\\'i}",
]

LAST_NAMES = [
    "Anderson", "Bauer", "Becker", "Bergmann", "Brown", "Chen", "Clark", "Davis", "Dubois", "Fischer",
    "Garcia", "Gonzalez", "Gruber", "Hansen", "Hoffmann", "Ivanov", "Johansson", "Johnson", "Kim", "Klein",
    "Kowalski", "Krause", "Lee", "Lopez", "Maier", "Martin", "Meier", "Meyer", "Miller", "Muller",
    "Nguyen", "Novak", "Patel", "Petrov", "Rossi", "Schmidt", "Schneider", "Schulz", "Silva", "Smith",
    "Taylor", "Thomas", "Wagner", "Walker", "Weber", "White", "Williams", "Wilson", "Wright", "Zhang",
    "M{\\\"u}ller", "Schr{\\\"o}der", "G{\\\"o}tz", "J{\\\"a}ger", "Kr{\\\"u}ger", "H{\\\"o}fler",
    "{\\L}uczak", "{\\'S}wi{\\k a}tek", "{\\'Z}ebrowski", "Brz{\\k e}czyszczykiewicz", "Dvo{\\v r}{\\'a}k", "Garc{\\'i}a", "Smr{\\v z}", "N{\\k e}mec",
]


def group_tree_metadata():
    lines = [
        "@comment{jabref-meta: saveOrderConfig:specified;author;false;title;false;year;false;}",
        "",
        "@comment{jabref-meta: groupsversion:3;}",
        "",
        "@comment{jabref-meta: groupstree:",
        "0 AllEntriesGroup:;",
        "1 StaticGroup:By status\\;2\\;1\\;0x8a8a8aff\\;\\;Manual reading buckets\\;;",
        "2 StaticGroup:To discuss\\;0\\;0\\;0xcc3333ff\\;chat\\;Entries for next lab meeting\\;;",
        "2 StaticGroup:Core reading\\;0\\;0\\;0x336699ff\\;book\\;Papers worth revisiting\\;;",
        "2 StaticGroup:Background\\;0\\;0\\;0x8a8a8aff\\;archive\\;General background material\\;;",
        "1 StaticGroup:Projects\\;0\\;1\\;0x336633ff\\;briefcase\\;Manual project folders\\;;",
        "2 StaticGroup:Literature review\\;0\\;0\\;0x4d3399ff\\;file_document\\;Survey and review material\\;;",
        "2 StaticGroup:Replication study\\;0\\;0\\;0x008080ff\\;flask\\;Replication candidates and notes\\;;",
        "2 StaticGroup:Teaching\\;0\\;0\\;0xe6994dff\\;school\\;Course reading shortlist\\;;",
        "1 AutomaticKeywordGroup:Keywords\\;2\\;keywords\\;,\\;>\\;1\\;\\;\\;Generated from hierarchical keywords\\;;",
        "1 AutomaticPersonsGroup:Authors\\;0\\;author\\;1\\;\\;\\;Generated from author last names\\;;",
        "1 SearchGroup:Recently published\\;0\\;year=2021 or year=2022 or year=2023 or year=2024 or year=2025\\;0\\;0\\;1\\;\\;\\;Recent publications\\;;",
        "1 SearchGroup:Machine learning reviews\\;0\\;keywords=Machine learning and keywords=Systematic review\\;0\\;0\\;1\\;\\;\\;Machine learning review papers\\;;",
        "1 SearchGroup:Empirical software engineering\\;0\\;keywords=Software engineering and keywords=Controlled experiment\\;0\\;0\\;1\\;\\;\\;Empirical software engineering papers\\;;",
        "}",
        "",
        "@comment{jabref-meta: groups-search-syntax-version:6.0-alpha_1}",
        "",
    ]
    return "\n".join(lines)


def topic_and_subtopic_for_entry(entry_number):
    topic_names = list(TOPICS)
    topic_index = (entry_number - 1) % len(topic_names)
    topic = topic_names[topic_index]
    subtopics = TOPICS[topic]
    subtopic_index = ((entry_number - 1) // len(topic_names)) % len(subtopics)
    return topic, subtopics[subtopic_index]


def method_for_entry(entry_number):
    return METHODS[((entry_number - 1) // 3) % len(METHODS)]


def read_status_for_entry(entry_number):
    return READ_STATUS[(entry_number - 1) % len(READ_STATUS)]


def keywords_for_entry(entry_number):
    topic, subtopic = topic_and_subtopic_for_entry(entry_number)
    keywords = [
        f"{topic}{KEYWORD_HIERARCHY_SEPARATOR}{subtopic}",
        method_for_entry(entry_number),
        read_status_for_entry(entry_number),
    ]

    if entry_number % 5 == 0:
        keywords.append("open science")

    if entry_number % 7 == 0:
        keywords.append("replication package")

    return f"{KEYWORD_SEPARATOR} ".join(keywords)


def authors_for_entry(entry_number):
    authors = []
    author_count = 2 + (entry_number % 3)

    for author_offset in range(author_count):
        first_name_index = (entry_number * 7 + author_offset * 11) % len(FIRST_NAMES)
        last_name_index = (entry_number * 13 + author_offset * 17) % len(LAST_NAMES)
        suffix = (entry_number + author_offset * 37) % 200
        authors.append(f"{FIRST_NAMES[first_name_index]} {LAST_NAMES[last_name_index]} {suffix:03d}")

    return " and ".join(authors)


def title_for_entry(entry_number):
    topic, subtopic = topic_and_subtopic_for_entry(entry_number)
    method = method_for_entry(entry_number)
    pattern = TITLE_PATTERNS[(entry_number - 1) % len(TITLE_PATTERNS)]
    return pattern.format(
        topic=topic,
        topic_lower=topic.lower(),
        subtopic=subtopic,
        subtopic_lower=subtopic.lower(),
        method=method,
        method_lower=method.lower(),
    )


def venue_for_entry(entry_number):
    return VENUES[(entry_number - 1) % len(VENUES)]


def entry_text(entry_number):
    year = 1995 + (entry_number - 1) % 31
    month = 1 + ((entry_number - 1) % 12)
    venue = venue_for_entry(entry_number)
    keywords = keywords_for_entry(entry_number)
    authors = authors_for_entry(entry_number)
    title = title_for_entry(entry_number)
    read_status = read_status_for_entry(entry_number)
    doi_prefix = 10_000 + (entry_number % 90_000)
    doi_suffix = 100_000 + entry_number

    return f"""@article{{id{entry_number:06d},
  title      = {{{title}}},
  author     = {{{authors}}},
  journal    = {{{venue}}},
  volume     = {{{1 + ((entry_number - 1) % 24)}}},
  number     = {{{1 + ((entry_number - 1) % 6)}}},
  pages      = {{{10 + ((entry_number - 1) % 180)}--{17 + ((entry_number - 1) % 180)}}},
  year       = {{{year}}},
  month      = {{{month}}},
  doi        = {{10.{doi_prefix}/jabref.synthetic.{doi_suffix}}},
  keywords   = {{{keywords}}},
  readstatus = {{{read_status}}},
}}

"""


with OUTPUT_FILE.open("w", encoding="utf-8") as file:
    file.write(group_tree_metadata())
    for entry_number in range(1, NUMBER_OF_ENTRIES + 1):
        file.write(entry_text(entry_number))
