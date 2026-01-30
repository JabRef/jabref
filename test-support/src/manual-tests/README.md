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
