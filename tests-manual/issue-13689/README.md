# [issue-13689: Refine "Search for unlinked local files" dialog](https://github.com/JabRef/jabref/issues/13689)

We have 1 manual test case here.

Only one library is provided, which contains 4 entries 3 of them have automatically found files that can be linked to and an empty one.

The test case shows that the "Search for unlinked local files" dialog can now show the user the entries that the files can be linked to.

## Steps to run the test case:

- open library `issue-13689/issue-13689.bib`
- click `Lookup - Search for unlinked local files`
- check that the dialog shows the 3 entries with automatically found files that can be linked to
- files that can be linked to many entries have a dropdown box that contains all the entries that the file can be linked to, and the user can select one of them to link the file to with, and an icon to jump to the entry in the entry editor
- files that can be linked to only one entry have the entry shown in the dialog, and one click on the entry will jump to the entry in the entry editor
