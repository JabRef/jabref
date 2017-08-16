#/bin/bash
VERSION=`grep "^version =" build.gradle | sed "s/^version = \"\(.*\)\".*/\1/"`
sed -i "s/4.0-dev/$VERSION/" snapcraft.yaml
docker run -v $(pwd):$(pwd) -t koppor/snapcraft-java-de:v1.3.0 sh -c "cd $(pwd) && snapcraft && snapcraft push *.snap --release edge"
