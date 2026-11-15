"""Generate deterministic legacy migration fixtures without replacing existing files."""

import argparse
from pathlib import Path


def group(level, kind, *fields):
    quoted_fields = [str(field).replace("\\", "\\\\").replace(";", "\\;") for field in fields]
    serialized = ";".join(quoted_fields) + ";"
    metadata = serialized.replace("\\", "\\\\").replace(";", "\\;")
    return f"{level} {kind}:" + metadata + ";\n"


def citation_key(number):
    # Deliberately include duplicate citation keys while keeping the entry count exact.
    if number % 25_000 == 0:
        number -= 1
    return f"Legacy{number:06d}"


def generate(path, count, separator):
    with path.open("x", encoding="utf-8") as output:
        for number in range(1, count + 1):
            marking = (
                f"[Owner{number % 8}:{number % 6}]",
                "starred",
                "note [Owner:6] more",
                "[Malformed]",
            )[number % 4]
            keywords = f"{separator} ".join([
                f"Topic{number % 20}",
                "printed",
                f"prio{1 + number % 3}",
                "qualityAssured",
                f"rank{1 + number % 5}",
                "read" if number % 2 == 0 else "skimmed",
                "relevant",
            ])
            output.write(f"""@Article{{{citation_key(number)},
  author = {{M{{\\\"u}}ller, Ada and Smith, Alan}},
  title = {{Legacy migration fixture entry {number}}},
  year = {{{1990 + number % 35}}},
  keywords = {{{keywords}}},
  groups = {{Existing membership}},
  __markedentry = {{{marking}}},
}}

""")
        metadata_separator = separator.replace(";", "\\;")
        output.write(f"@Comment{{jabref-meta: keywordSeparator:{metadata_separator};}}\n\n")
        output.write("@Comment{jabref-meta: groupsversion:3;}\n\n")
        output.write("@Comment{jabref-meta: groupstree:\n")
        output.write("0 AllEntriesGroup:;\n")
        output.write(group(1, "ExplicitGroup", "All legacy entries", 0,
                           *(citation_key(n) for n in range(1, count + 1))))
        output.write(group(1, "ExplicitGroup", "Including parent", 2,
                           *(citation_key(n) for n in range(2, count + 1, 2))))
        output.write(group(2, "ExplicitGroup", "Refining child", 1,
                           *(citation_key(n) for n in range(4, count + 1, 4))))
        for bucket in range(100):
            output.write(group(1, "ExplicitGroup", f"Bucket {bucket:03d}", 0,
                               *(citation_key(n) for n in range(bucket + 1, count + 1, 100))))
        output.write(group(1, "ExplicitGroup", "Missing and repeated keys", 0,
                           "MissingCitationKey", citation_key(1), citation_key(1)))
        output.write(group(1, "ExplicitGroup", "Empty legacy group", 0))
        output.write(group(1, "ExplicitGroup", "Escaped; group", 0, citation_key(1)))
        output.write(group(1, "StaticGroup", "Existing membership", 0, 1, "", "", ""))
        output.write(group(1, "SmartGroup", "Imported entries", 0, 1, "", "", ""))
        output.write(group(1, "KeywordGroup", "Legacy keyword group", 0, "keywords", "Topic1", 0, 0))
        output.write(group(1, "KeywordGroup", "Legacy regex group", 0, "keywords", "Topic[0-9]+", 0, 1))
        output.write(group(1, "SearchGroup", "Legacy search group", 0, "author=Smith", 0, 0))
        output.write(group(1, "AutomaticPersonsGroup", "Authors", 0, "author", 1, "", "", ""))
        output.write(group(1, "AutomaticKeywordGroup", "Topics", 2, "keywords", separator, ">", 1, "", "", ""))
        output.write("}\n")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--entries", type=int, default=100_000)
    parser.add_argument("--separator", choices=[",", ";"], default=",")
    parser.add_argument("--output", type=Path, default=Path("generated-large-legacy-library.bib"))
    arguments = parser.parse_args()
    if arguments.entries < 1:
        parser.error("--entries must be positive")
    generate(arguments.output, arguments.entries, arguments.separator)
    print(f"Created {arguments.output}: {arguments.entries:,} entries, "
          f"{arguments.output.stat().st_size:,} bytes, separator {arguments.separator!r}")


if __name__ == "__main__":
    main()
