#!/bin/bash
set -e

cd "$(dirname "$(readlink -f "$BASH_SOURCE")")/.."

# see also ".mailmap" for how email addresses and names are deduplicated
{
	cat <<-'EOF'
	# This file lists all individuals having contributed content to the repository.
	# For how it is generated, see `scripts/generate-authors.sh`.
	EOF

	# old manual entries
	read -d '' authors <<-"EOF" || true
	Michel Baylac
	Cyrille d'Haese
	Ellen Reitmayr
	Michael Beckmann
	Oliver Beckmann
	Fedor Bezrukov
	Fabian Bieker
	Aaron Chen
	Fabrice Dessaint
	Nathan Dunn
	Alexis Gallagher
	David Gleich
	Behrouz Javanmardi
	Bernd Kalbfuss
	Martin Kähmer
	Ervin Kolenovic
	Krzysztof A. Kościuszkiewicz
	Christian Kopf
	Jeffrey Kuhn
	Uwe Kuehn
	Felix Langner
	Stephan Lau
	Alex Montgomery
	Saverio Mori
	Ambrogio Oliva
	Stephan Rave
	John Relph
	Hannes Restel
	Moritz Ringler
	Rudolf Seemann
	Toralf Senger
	Manuel Siebeneicher
	Mike Smoot
	Ulrich Stärk
	Martin Stolle
	David Weitzman
	John Zedlewski
	Samin Muhammad Ridwanul Karim
	Stefan Robert
	Bernhard Tempel
	EOF

    # authors %aN = author name

    # co-authors
    coauthors=$(git log --grep=Co-authored  | grep "Co-au" | sed "s/.*Co-authored-by: \(.*\) <.*/\1/")
	echo -e "$authors\n$(git log --format='%aN')\n$coauthors" | LC_ALL=C.UTF-8 sort --unique --ignore-case
} > AUTHORS

