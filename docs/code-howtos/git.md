# Git

## Why Semantic Merge?

In JabRef, we aim to minimize user interruptions when collaborating on the same `.bib` library file using Git. To achieve this, we go beyond Git’s default line-based syntactic merging and implement our own semantic merge logic that understands the structure of BibTeX entries.

This means:

* Even if Git detects conflicting lines,
* JabRef is able to recognize that both sides are editing the same BibTeX entry,
* And determine—at the field level—whether there is an actual semantic conflict.

## Merge Example

The following example illustrates a case where Git detects a conflict, but JabRef is able to resolve it automatically.

### Base Version

```bibtex
@article{a,
  author = {don't know the author},
  doi = {xya},
}

@article{b,
  author = {don't know the author},
  doi = {xyz},
}
```

### Bob's Side

Bob reorders the entries and updates the author field of entry b:

```bibtex
@article{b,
  author = {author-b},
  doi = {xyz},
}

@article{a,
  author = {don't know the author},
  doi = {xya},
}
```

### Alice's Side

Alice modifies the author field of entry a:

```bibtex
@article{a,
  author = {author-a},
  doi = {xya},
}

@article{b,
  author = {don't know the author},
  doi = {xyz},
}
```

### Merge Outcome

When Alice runs git pull, Git sees that both branches have modified overlapping lines (due to reordering and content changes) and reports a syntactic conflict.

However, JabRef is able to analyze the entries and determine that:

* Entry a was modified only by Alice.
* Entry b was modified only by Bob.
* There is no conflict at the field level.
* The order of entries in the file does not affect BibTeX semantics.

Therefore, JabRef performs an automatic merge without requiring manual conflict resolution.

## Related Test Cases

The semantic conflict detection and merge resolution logic is covered by:

* `org.jabref.logic.git.merge.SemanticMergeAnalyzerTest#semanticEntryLevelConflicts`
* `org.jabref.logic.git.merge.SemanticMergeAnalyzerTest#semanticFieldLevelConflicts`.

## Conflict Scenarios

The following table describes when semantic merge in JabRef should consider a situation as conflict or not during a three-way merge.

### Entry-level Conflict Cases (E-series)

Each side (Base, Local, Remote) can take one of the following values:

* `–`: entry does not exist (null)
* `S`: same as base
* `M`: modified (fields changed)

> Note: Citation key is used as the entry identifier. Renaming a citation key is currently treated as deletion + addition and not supported as a standalone diff.

| TestID | Base          | Local             | Remote            | Description                                                    | Common Scenario                                                                | Conflict |
| ------ | ------------- | ----------------- | ----------------- | -------------------------------------------------------------- | ------------------------------------------------------------------------------ | -------- |
| E01    | –             | –                 | –                 | All null                                                       | Entry absent on all sides                                                      | No       |
| E02    | –             | –                 | M                 | Remote added entry                                             | Accept remote addition                                                         | No       |
| E03    | –             | M                 | –                 | Local added entry                                              | Keep local addition                                                            | No       |
| E04    | –             | M                 | M                 | Both added entry with same citation key                        | If content is identical: no conflict; else: compare fields                     | Depends  |
| E05    | S             | –                 | –                 | Both deleted                                                   | Safe deletion                                                                  | No       |
| E06    | S             | –                 | S                 | Local deleted, remote unchanged                                | Respect local deletion                                                         | No       |
| E07    | S             | –                 | M                 | Local deleted, remote modified                                 | One side deleted, one side changed                                             | Yes      |
| E08    | S             | S                 | –                 | Remote deleted, local unchanged                                | Accept remote deletion as no conflict                                          | No       |
| E09    | S             | S                 | S                 | All sides equal                                                | No changes                                                                     | No       |
| E10    | S             | S                 | M                 | Remote modified, local unchanged                               | Accept remote changes                                                          | No       |
| E11    | S             | M                 | –                 | Remote deleted, local modified                                 | One side deleted, one side changed                                             | Yes      |
| E12    | S             | M                 | S                 | Local modified, remote unchanged                               | Accept local changes                                                           | No       |
| E13    | S             | M                 | M                 | Both sides modified                                            | If changes are equal or to different fields: no conflict; else: compare fields | Depends  |
| E14a   | `@article{a}` | `@article{b}`     | unchanged         | Local renamed citation key                                     | Treated as deletion + addition                                                 | Yes      |
| E14b   | `@article{a}` | unchanged         | `@article{b}`     | Remote renamed citation key                                    | Treated as deletion + addition                                                 | Yes      |
| E14c   | `@article{a}` | `@article{b}`     | `@article{c}`     | Both renamed to different keys                                 | Treated as deletion + addition                                                 | Yes      |
| E15    | –             | `@article{a,...}` | `@article{a,...}` | Both added entry with same citation key, but different content | Duplicate citation key from both sides                                         | Yes      |

---

### Field-level Conflict Cases (F-series)

Each individual field (such as title, author, etc.) may have one of the following statuses relative to the base version:

* Unchanged: The field value is exactly the same as in the base version.
* Changed: The field value is different from the base version.
* Deleted: The field existed in the base version but is now missing (i.e., null).
* Added: The field did not exist in the base version but was added in the local or remote version.

| TestID | Base                          | Local               | Remote              | Description                                 | Conflict |
|--------|-------------------------------|---------------------|---------------------|---------------------------------------------|----------|
| F01    | U                             | U                   | U                   | All equal                                   | No       |
| F02    | U                             | U                   | C                   | Remote changed                              | No       |
| F03    | U                             | C                   | U                   | Local changed                               | No       |
| F04    | U                             | C                   | C (=)               | Both changed to same value                  | No       |
| F05    | U                             | C                   | C (≠)               | Both changed same field, different values   | Yes      |
| F06    | U                             | D                   | U                   | Local deleted                               | No       |
| F07    | U                             | U                   | D                   | Remote deleted                              | No       |
| F08    | U                             | D                   | D                   | Both deleted                                | No       |
| F09    | U                             | C                   | D                   | Local changed, remote deleted               | Yes      |
| F10    | U                             | D                   | C                   | Local deleted, remote changed               | Yes      |
| F11    | –                             | –                   | –                   | Field missing on all sides                  | No       |
| F12    | –                             | A                   | –                   | Local added field                           | No       |
| F13    | –                             | –                   | A                   | Remote added field                          | No       |
| F14    | –                             | A                   | A (=)               | Both added same field with same value       | No       |
| F15    | –                             | A                   | A (≠)               | Both added same field with different values | Yes      |
| F16    | U                             | C                   | D                   | Changed in local, deleted in remote         | Yes      |
| F17    | U                             | D                   | C                   | Deleted in local, changed in remote         | Yes      |
| F18    | –                             | A                   | C                   | No base, both sides added different values  | Yes      |
| F19    | `{title=Hello, author=Alice}` | reordered           | unchanged           | Field order changed only                    | No       |
| F20    | `@article{a}`                 | `@inproceedings{a}` | unchanged           | Entry type changed in local                 | No       |
| F21    | `@article{a}`                 | `@book{a}`          | `@inproceedings{a}` | Both changed entry type differently         | Yes      |
