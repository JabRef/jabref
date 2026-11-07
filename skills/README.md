# Agent Skills for JabRef

This directory contains [Agent Skills](https://agentskills.io/) usable by AI agents such as Claude Code, Cursor, and other tools supporting the SKILL.md convention.

Skills are grouped in two categories:

- `users/` — skills for working *with* JabRef and its CLI `jabkit`: managing BibTeX/biblatex libraries, extracting references from PDF papers, fetching entries.
- `developers/` — skills for working *on* the JabRef codebase.

## Installation

Install all skills:

```bash
npx skills add JabRef/jabref
```

Install a single skill:

```bash
npx skills add JabRef/jabref --skill pdf-to-bibtex
```

## Contributing

Each skill is a directory containing a `SKILL.md` with YAML frontmatter (`name`, `description`) followed by instructions for the agent. Keep instructions in sync with the actual `jabkit` commands (see `jabkit/src/main/java/org/jabref/toolkit/commands/`).
