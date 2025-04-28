# Libraries and PDFs for testing search functionality

This folder contains manual use-cases for testing the search functionality.

It is used at the test code `org.jabref.logic.search.DatabaseSearcherWithBibFilesTest`.

The `.pdf` files are generated from the `.tex` files using `pdflatex`.

Compile all files in cmd.exe:

    for %f in (*.tex) do pdflatex "%f"
