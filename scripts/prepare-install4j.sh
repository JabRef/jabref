#!/bin/bash

# ensure that downloads directory exists
if [ ! -d ~/downloads ]; then
  mkdir ~/downloads
fi

# ensure that tar archive of install4j exists
cd ~/downloads
wget --quiet -nc http://download-keycdn.ej-technologies.com/install4j/install4j_unix_7_0_3.tar.gz

# extract tar archive of install4j into the source directory of JabRef
cd ~/jabref
# version 7.0.1 is NOT zipped any more - old command line: "-xzf"
tar -xf ~/downloads/install4j_unix_7_0_3.tar.gz
# fix directory name (until install4j 6.1.5 it was install4j6
mv install4j7.0.3 install4j7

# fetch JREs
if [ ! -d ~/.install4j7/jres/ ]; then
  mkdir -p ~/.install4j7/jres/
fi
cd ~/.install4j7/jres/
wget --quiet -nc https://files.jabref.org/jres/windows-x86-1.8.0_144.tar.gz
wget --quiet -nc https://files.jabref.org/jres/windows-amd64-1.8.0_144.tar.gz
wget --quiet -nc https://files.jabref.org/jres/macosx-amd64-1.8.0_144_unpacked.tar.gz
