---
parent: Requirements
---
# Git

## GitHub personal access token push access must be verifiable before sharing
`req~git.share.personal-access-token-verification~1`

Verification happens in the GitHub sharing dialog before the library is shared.

Needs: impl

## User can merge .bib files using Git merge driver
`feat~jabkit.git.merge-driver~1`

`jabkit git merge-driver BASE CURRENT OTHER` must perform the semantic three-way merge and write the result to `CURRENT`, following Git's merge driver contract (`%O %A %B`).
It must exit with `0` when all changes merge cleanly and with `1` when semantic conflicts remain.
Conflicting entries must keep the `CURRENT` version and be reported on stderr by citation key.

Needs: impl

## Git pull must support unrelated histories
`req~git.pull.unrelated-histories~1`

Git pull must support a local library and its configured remote when their commit histories have no common ancestor.

Needs: impl

## Git push must succeed when configured remote has no branches
`req~git.push.empty-remote~1`

Git push must publish the current branch and configure its upstream when the configured remote has no branches.

Needs: impl

## Git push must report rejected remote update
`req~git.push.rejected-update-reporting~1`

Git push must report a rejected remote update to the user.

Needs: impl

## Git commit must preview changes in current library
`req~git.commit.preview-current-library~1`

Before committing a Git-tracked library, JabRef must let the user preview semantic changes from the committed version to the saved current file for that library.
Selecting a groups tree change must display the old and new trees with highlighted differences and respect the selected word or character highlighting mode.

Needs: impl

## Git commit must not depend on remote access
`req~git.commit.remote-independent~1`

Git commit must offer the uncommitted changes of the local library even when no remote is configured or the configured remote cannot be reached.

Needs: impl

## JabRef must offer repository initialization when committing untracked library
`req~git.commit.initialize-repository~1`

When a user commits a library that is not inside a Git repository, JabRef must offer to initialize a repository in the library's directory and commit the library file there.
Only the library file and the generated `.gitignore` are committed, so unrelated files in that directory stay untracked.
Declining the offer must leave the directory unchanged, because the user may want to clone an existing repository into it instead.

Needs: impl, utest

## JabRef must offer saving changes before committing a library
`req~git.commit.unsaved-changes~1`

When a user commits a library with unsaved changes and autosave is disabled,
JabRef must offer to write those changes before committing,
and must allow committing only what is already on disk.
Git commits operate on the file on disk, so unsaved changes would otherwise be silently left out,
but the user may deliberately want to commit only the saved work.

Needs: impl, utest

<!-- markdownlint-disable-file MD022 -->
