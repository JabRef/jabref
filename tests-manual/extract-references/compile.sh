#!/bin/sh
set -e

cd paper1
pdflatex paper1.tex
cd ..

cd paper2
pdflatex paper2.tex
cd ..

cd paper3
pdflatex paper3.tex
cd ..

cd main-paper
pdflatex main.tex
bibtex main
pdflatex main.tex
pdflatex main.tex
cd ..
