#!/bin/bash

# fetch intall4j binary
if [ ! -d ~/downloads ]; then
  mkdir ~/downloads
fi
cd ~/downloads
wget --quiet -nc --show-progress http://download-keycdn.ej-technologies.com/install4j/install4j_unix_7_0_8.tar.gz

# fetch JREs
if [ ! -d ~/.install4j7/jres ]; then
  mkdir -p ~/.install4j7/jres
fi
cd ~/.install4j7/jres
wget --quiet -nc https://files.jabref.org/jres/windows-x86-1.8.0_172.tar.gz
wget --quiet -nc https://files.jabref.org/jres/windows-amd64-1.8.0_172.tar.gz
wget --quiet -nc https://files.jabref.org/jres/macosx-amd64-1.8.0_172_unpacked.tar.gz
