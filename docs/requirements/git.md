---
parent: Requirements
---
# Git

## GitHub personal access token verification
`req~git.share.personal-access-token-verification~1`

The GitHub sharing dialog must allow users to verify that their personal access token has push access to the configured GitHub repository before sharing a library.

Needs: impl

## Git pull with unrelated histories
`req~git.pull.unrelated-histories~1`

Git pull must support a local library and its configured remote when their commit histories have no common ancestor.

Needs: impl

## Git push to an empty remote
`req~git.push.empty-remote~1`

Git push must publish the current branch and configure its upstream when the configured remote has no branches.

Needs: impl

## Git push rejection reporting
`req~git.push.rejected-update-reporting~1`

Git push must report a rejected remote update to the user.

Needs: impl

## Git commit previews changes in the current library
`req~git.commit.preview-current-library~1`

Before committing a Git-tracked library, JabRef should let the user preview semantic changes from the committed version to the saved current file for that library.

Needs: impl

## Committing does not depend on the remote
`req~git.commit.remote-independent~1`

Git commit must offer the uncommitted changes of the local library even when no remote is configured or the configured remote cannot be reached.

Needs: impl

## Committing a library that is not under version control
`req~git.commit.initialize-repository~1`

When a user commits a library that is not inside a Git repository, JabRef must offer to initialize a repository in the library's directory and commit the library file there.
Only the library file and the generated `.gitignore` are committed, so unrelated files in that directory stay untracked.
Declining the offer must leave the directory unchanged, because the user may want to clone an existing repository into it instead.

Needs: impl, utest

## Committing a library with unsaved changes
`req~git.commit.unsaved-changes~1`

Git commits operate on the file on disk, so unsaved changes are not part of a commit.
With autosave disabled the user decides when the library is written,
so JabRef must let them choose between writing those changes first and committing what is already on disk,
because the user may deliberately want to commit only the saved work.

Needs: impl, utest

<!-- markdownlint-disable-file MD022 -->
