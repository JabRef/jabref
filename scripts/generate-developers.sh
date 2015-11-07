#!/bin/bash
set -e

#Check environment variables
if [ -z $GITHUB_OAUTH_TOKEN ]; then
    echo "GITHUB_OAUTH_TOKEN not set";
    exit 1;
fi;


cd "$(dirname "$(readlink -f "$BASH_SOURCE")")/.."

#github requires authorization on JabRef's developers team (id 727640) even if the team is public
#github returns a formatted json. Therefore, we use plain sed to get the field content instead of using jq
userURLs=`curl --silent -H "Authorization: token $GITHUB_OAUTH_TOKEN" https://api.github.com/teams/727640/members | grep \"url\" | sed 's|.*: "\(.*\)".*|\1|'`
#now we have a list of URLs each pointing to a team member

{
    #generate header
    cat <<-'EOF'
	# This file lists all individuals being part of JabRef's developers team
	# For how it is generated, see `scripts/generate-developers.sh`.
	EOF

    #list all developer names alphabetically (sorted by first name)
    {
        #iterate through all URLss and fetch name info
        for userURL in $userURLs; do
            name=`curl --silent $userURL | grep "name" | sed 's|.*: "\(.*\)".*|\1|'`
            echo $name
        done
    } | LC_ALL=C.UTF-8 sort

} > DEVELOPERS