# File Transfer Between Bib Entries

### File is reachable and should not be copied
`req~logic.externalfiles.file-transfer.reachable-no-copy~1`
When a linked file is reachable from the target context, the system must adjust the relative path in the target entry but must not copy the file again.

Needs: impl

### File is not reachable, but the path is the same
`req~logic.externalfiles.file-transfer.not-reachable-same-path~1`
When a linked file is not reachable from the target context, and the relative path in both source and target entry is the same, the file must be copied to the target context.

Needs: impl

### File is not reachable, and a different path is used
`req~logic.externalfiles.file-transfer.not-reachable-different-path~1`
When a linked file is not reachable from the target context, and the relative path differs between source and target entries, the file must be copied and the directory structure must be created to preserve the relative link.

Needs: impl

<!-- markdownlint-disable-file MD022 -->
