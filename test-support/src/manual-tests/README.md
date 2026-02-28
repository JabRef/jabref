# Manual Tests

For some issues, we provide manual test cases here. These test cases describe how to reproduce the issue or verify the corresponding changes. This document explains how to run them.

Each subfolder typically corresponds to a single issue.

## [issue-9798: Auto-link](https://github.com/JabRef/jabref/issues/9798)

We have 3 manual test cases here.

- example1: the minimal case where the broken linked file is auto-linked with files searched via the citation key
- example2: the case shows that multiple entries can be auto-linked at the same time. It also shows that auto-link may create extra linked files if they are found by citation index and not used to fix broken linked files.
- example3: this case shows a broken linked file is auto-linked with files searched via the broken linked file names

Steps to run example 1:

- open library `example1/issue-9798_1.bib`
- open the only entry with citation key `minimal`
- change to `General` tab to check the linked file `minimal.pdf` is broken (in yellow color), and `subdir/minimal.pdf` is listed below as a linked file candidate
- click `Quality - Automatically set file links`
- the broken linked file `minimal.pdf` is auto-linked to `subdir/minimal.pdf`

Steps to run example 2:

- open library `example2/issue-9798_2.bib`
- open the first entry with citation key `newton1833philosophiae`
- change to `General` tab to check the linked file `newton1833philosophiae.pdf` is broken (in yellow color), and `subdir2/newton1833philosophiae.pdf` is listed below as a linked file candidate
- close the entry editor
- open the second entry with citation key `einstein1935can`
- change to `General` tab to check the linked file `einstein1935can.pdf` is broken (in yellow color), and `subdir1/einstein1935can.pdf` and `subdir1/einstein1935can_another_part.pdf` are listed below as linked file candidates
- close the entry editor
- select both entry by left-click with the Shift key held down
- click `Quality - Automatically set file links`
- the broken linked file `newton1833philosophiae.pdf` and `einstein1935can.pdf` are auto-linked to their corresponding broken linked file with the same name. `subdir1/einstein1935can_another_part.pdf` are added to the entry `einstein1935can`.

Steps to run example 3:

- open library `example3/issue-9798_3.bib`
- open the only entry with citation key `IDoNotCare`
- change to `General` tab to check the linked file `minimal.pdf` is broken (in yellow color), and `subdir/minimal.pdf` is listed below as a linked file candidate
- click `Quality - Automatically set file links`
- the broken linked file `minimal.pdf` is auto-linked to `subdir/minimal.pdf`

## [issue-13689: Refine "Search for unlinked local files" dialog](https://github.com/JabRef/jabref/issues/13689)
We have 1 manual test case here.

Only one library is provided, which contains 4 entries 3 of them have automatically found files that can be linked to and an empty one.

The test case shows that the "Search for unlinked local files" dialog can now show the user the entries that the files can be linked to.

### Steps to run the test case:
- open library `issue-13689/issue-13689.bib`
- click `Lookup - Search for unlinked local files`
- check that the dialog shows the 3 entries with automatically found files that can be linked to
- files that can be linked to many entries have a dropdown box that contains all the entries that the file can be linked to, and the user can select one of them to link the file to with, and an icon to jump to the entry in the entry editor
- files that can be linked to only one entry have the entry shown in the dialog, and one click on the entry will jump to the entry in the entry editor
