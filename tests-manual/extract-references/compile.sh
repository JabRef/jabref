#!/bin/sh
set -e

cd paper1
pdflatex main.tex
bibtex main
pdflatex main.tex
pdflatex main.tex
cd ..
