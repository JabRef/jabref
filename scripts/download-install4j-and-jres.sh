#!/bin/bash

# fetch intall4j binary
if [ ! -d ~/downloads ]; then
  mkdir ~/downloads
fi
cd ~/downloads
wget --quiet -nc --show-progress https://download-gcdn.ej-technologies.com/install4j/install4j_unix_8_0.tar.gz
