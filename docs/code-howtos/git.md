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
* `org.jabref.logic.git.util.SemanticMergerTest` 
* `org.jabref.logic.git.util.SemanticConflictDetectorTest`.

## Conflict Scenarios

The following table describes when semantic merge in JabRef should consider a situation as conflict or not during a three-way merge.

| ID   | Base                       | Local Change                       | Remote Change                      | Result |
|------|----------------------------|------------------------------------|------------------------------------|--------|
| T1   | Field present              | (unchanged)                        | Field modified                     | No conflict. The local version remained unchanged, so the remote change can be safely applied. |
| T2   | Field present              | Field modified                     | (unchanged)                        | No conflict. The remote version did not touch the field, so the local change is preserved. |
| T3   | Field present              | Field changed to same value        | Field changed to same value        | No conflict. Although both sides changed the field, the result is identical—therefore, no conflict. |
| T4   | Field present              | Field changed to A                 | Field changed to B                 | Conflict. This is a true semantic conflict that requires resolution. |
| T5   | Field present              | Field deleted                      | Field modified                     | Conflict. One side deleted the field while the other updated it—this is contradictory. |
| T6   | Field present              | Field modified                     | Field deleted                      | Conflict. Similar to T5, one side deletes, the other edits—this is a conflict. |
| T7   | Field present              | (unchanged)                        | Field deleted                      | No conflict. Local did not modify anything, so remote deletion is accepted. |
| T8   | Entry with fields A and B  | Field A modified                   | Field B modified                   | No conflict. Changes are on separate fields, so they can be merged safely. |
| T9   | Entry with fields A and B  | Field order changed                | Field order changed differently    | No conflict. Field order is not semantically meaningful, so no conflict is detected. |
| T10  | Entries A and B            | Entry A modified                   | Entry B modified                   | No conflict. Modifications are on different entries, which are always safe to merge. |
| T11  | Entry with existing fields | (unchanged)                        | New field added                    | No conflict. Remote addition can be applied without issues. |
| T12  | Entry with existing fields | New field added with value A       | New field added with value B       | Conflict. One side added while the other side modified—there is a semantic conflict. |
| T13  | Entry with existing fields | New field added                    | (unchanged)                        | No conflict. Safe to preserve the local addition. |
| T14  | Entry with existing fields | New field added with value A       | New field added with value A       | No conflict. Even though both sides added it, the value is the same—no need for resolution. |
| T15  | Entry with existing fields | New field added with value A       | New field added with value B       | Conflict. The same field is introduced with different values, which creates a conflict. |
| T16  | (entry not present)        | New entry with author A            | New entry with author B            | Conflict. Both sides created a new entry with the same citation key, but the fields differ. |
| T17  | (entry not present)        | New entry with identical fields    | New entry with identical fields    | No conflict. Both sides created a new entry with the same citation key and identical fields, so it can be merged safely. |
