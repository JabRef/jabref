#!/bin/bash

# ensure that downloads directory exists
if [ ! -d ~/downloads ]; then
  mkdir ~/downloads
fi

# ensure that tar archive of install4j exists
cd ~/downloads
wget --quiet -nc http://download-keycdn.ej-technologies.com/install4j/install4j_unix_6_1_6.tar.gz

# extract tar archive of install4j into the source directory of JabRef
cd ~/jabref
# version 6.1.6 is NOT zipped any more - old command line: "-xzf"
tar -xf ~/downloads/install4j_unix_6_1_6.tar.gz
# fix directory name (until install4j 6.1.5 it was install4j6
mv install4j6.1.6 install4j6

# fetch JREs
if [ ! -d ~/.install4j6/jres/ ]; then
  mkdir -p ~/.install4j6/jres/
fi
cd ~/.install4j6/jres/
wget --quiet -nc https://files.jabref.org/jres/windows-x86-1.8.0_131.tar.gz
wget --quiet -nc https://files.jabref.org/jres/windows-amd64-1.8.0_131.tar.gz
wget --quiet -nc https://files.jabref.org/jres/macosx-amd64-1.8.0_131_unpacked.tar.gz
