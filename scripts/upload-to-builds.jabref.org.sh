#!/bin/bash

# We assume that there is a single build in build/releases
# We take out the branch name from the first matching file and then upload everything

# just to be sure
branch="snapshot"

# simple solution to treat first file matching a pattern
# hint by http://unix.stackexchange.com/a/156207/18033
for buildfile in build/releases/*--snapshot--*; do
  # the last "--" part is the branch name
  branch=`echo $buildfile | sed "sX.*--\(.*\)--.*X\1X"`
  break;
done

for buildfile in build/releases/*--snapshot--*.jar; do
  # remove build/releases/ from the filename
  jarname=`echo $buildfile | sed "sXbuild/releases/XX"`
  break;
done

# now the branch name is in the variable "branch"

command="cd www/\n"

# if there was a branch determined, create that directory
# the for returns the literal string "build/releases/*--snapshot--*" if no file was found
# then, "snapshot" is extracted
if [ "snapshot" != "$branch" ] ; then
  # change into dir and delete old snapshots
  command="${command}mkdir $branch\ncd $branch\nrm *.dmg\nrm *.jar\nrm *.exe\n"
fi

# only upload JabRef*, not md5sums, updates.xml, etc.
command="${command}mput build/releases/JabRef*\n"

# create symlink ...--latest.jar to latest version
command="${command}symlink ${jarname} /www/${branch}/JabRef--${branch}--latest.jar\n"

command="${command}exit\n"

# now $command is complete

echo -e "$command" | sftp -P 9922 builds_jabref_org@build-upload.jabref.org
