#!/bin/bash

# ensure that downloads directory exists
if [ ! -d ~/downloads ]; then
  mkdir ~/downloads
fi

# ensure that tar archive of install4j exists
cd ~/downloads
wget --quiet -nc http://download-keycdn.ej-technologies.com/install4j/install4j_unix_6_1_5.tar.gz

# extract tar archive of install4j into the source directory of JabRef
cd ~/jabref
tar -xzf ~/downloads/install4j_unix_6_1_5.tar.gz

# fetch JREs
if [ ! -d ~/.install4j6/jres/ ]; then
  mkdir -p ~/.install4j6/jres/
fi
cd ~/.install4j6/jres/
wget --quiet -nc https://files.jabref.org/jres/windows-x86-1.8.0_121.tar.gz
wget --quiet -nc https://files.jabref.org/jres/windows-amd64-1.8.0_121.tar.gz
wget --quiet -nc https://files.jabref.org/jres/macosx-amd64-1.8.0_121_unpacked.tar.gz
