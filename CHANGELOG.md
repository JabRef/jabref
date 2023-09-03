# Changelog

All notable changes to this project will be documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).
The project is versioned using [CalVer](https://calver.org/).

## [Unreleased]

### Added

- Added checker "Escaped Ampersands": `check_ampersands.py` which checks all CSV journals in the folder `journals` to make
sure all instances of ampersands are unescaped

### Changed

- `.github/workflows/tests.yml` contains the script `check_ampersands.py`
- Minor format changes in `README.md` and `LISENSE.md` as the old GitHub actions check was already failing
- Found an escaped ampersands using the new script in `journal_abbreviations_dainst.csv` so this was amended.


### Removed

- `[;<frequency>]` was removed, because it was used very seldom - and the data should be collected at other places.

## 2021-09

Initial tagged release

<!-- markdownlint-disable-file MD012 MD024 MD033 -->

[Unreleased]: https://github.com/JabRef/abbrv.jabref.org/compare/2021-09...main
