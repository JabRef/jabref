#/bin/bash
VERSION=`grep "^version =" build.gradle | sed "s/^version = \"\(.*\)\".*/\1/"`
sed -i "s/4.0-dev/$VERSION/" snapcraft.yaml
docker run -v $(pwd):$(pwd) -t koppor/snapcraft-java-de:v1.2.0 sh -c "cd $(pwd) && LANG=C.UTF-8 LC_ALL=C.UTF-8 snapcraft && LANG=C.UTF-8 LC_ALL=C.UTF-8 snapcraft push *.snap --release edge"
