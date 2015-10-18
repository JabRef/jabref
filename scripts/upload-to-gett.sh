#!/bin/bash

branch=`git symbolic-ref -q --short HEAD`

#enable snapshot upload only for master and upload_to_ge.tt testing branch
if [ \( "$branch" != "master" \) -a \( "$branch" != "upload_to_ge.tt" \) ]; then
    echo "Current branch is not master or upload_to_ge.tt"
    exit 1;
fi;

#change to directory of script
#see http://stackoverflow.com/a/10348989/873282
cd "$(dirname ${BASH_SOURCE[0]})"

#[TickTick](https://github.com/kristopolous/TickTick) doesn't work as it is not possible to cleanly iterate over collections which have subitems
#Therefore, we use [JQ](https://stedolan.github.io/jq/)

#Download linux version
if [ ! -x "./jq" ]; then
    wget -q https://github.com/stedolan/jq/releases/download/jq-1.5/jq-linux64
    #`wget -o jq` doesn't work at CircleCI - therefore, we do it manually
    mv jq-linux64 jq
    chmod u+x ./jq
fi;
#Download windows version
if [ ! -e "./jq.exe" ]; then
    wget -o "jq.exe" -q https://github.com/stedolan/jq/releases/download/jq-1.5/jq-win64.exe
fi;

#test if jq works
res=`echo '{"error":"access denied"}' | ./jq -r .error`
if [ "$res" != "access denied" ]; then
    echo "jq doesn't work"
    exit 3
fi;

#Global configuration
SHARE_NAME=5UUPRGQ2

#Check command line parameters
if [ -z $GETT_APIKEY ]; then
    echo "GETT_APIKEY not set";
    exit 1;
fi;

if [ -z $GETT_EMAIL ]; then
    echo "GETT_EMAIL not set";
    exit 1;
fi;

if [ -z $GETT_PASSWORD ]; then
    echo "GETT_PASSWORD not set";
    exit 1;
fi;

function checkResultForError {
    if [ -z "$1" ]; then
        # no error if result is empty
        return;
    fi;
    if [ "$1" == "computer says yes" ]; then
        # this string indicates that it isn't an error
        return
    fi;
    errorRes=`echo "$1" | ./jq -r .error`
    if [ $? -gt 0 ]; then
        # indicates that it is not a JSON string
        echo "Could not parse JSON: $1"
        echo "Assuming that everything is allright"
        return
    fi;
    if [ -z "$errorRes" ]; then
        # everyhting allright: .error not found
        return
    fi;
    # on windows, it can still be "null", therefore this "null" check
    if [ "$errorRes" != "null" ]; then
        echo "Error: ${errorRes}"
        exit 2
    fi
}

## LOGIN

# create DATA for https://open.ge.tt/1/doc/rest#users/login
read -d '' DATA << EOF
{
    "apikey":"${GETT_APIKEY}",
    "email":"$GETT_EMAIL",
    "password":"$GETT_PASSWORD"
}
EOF

baseURL="https://open.ge.tt/1/files/${SHARE_NAME}/"

# make DATA a one line variable
data=`echo $DATA | tr '\n' ' ' | sed 's/"/\\"/g'`

echo "Login..."
loginResult=`curl --silent -X POST --data "${data}" https://open.ge.tt/1/users/login`
#echo "LOGINRESULT:"
#echo "${loginResult}"
checkResultForError "$loginResult"
accesstoken=`echo $loginResult | ./jq -r .accesstoken`

if [ -z "$accesstoken" ]; then
    echo "Could not parse access token"
    exit 3;
fi

#get all shares/
#required if share changes
#You can also see the share id in the URL. E.g., http://ge.tt/5UUPRGQ2/v/0
#res=`curl --silent https://open.ge.tt/1/shares?accesstoken={${accesstoken}}`
#echo "SHARESINFO:"
#echo "$res"

echo "Get share info..."
shareInfo=`curl --silent https://open.ge.tt/1/shares/"${SHARE_NAME}"?accesstoken={"${accesstoken}"}`
#echo "SHAREINFO:"
#echo "$shareInfo"
checkResultForError "$shareInfo"

# the final sed is to get rid off windows' CR
fileids=`echo $shareInfo | ./jq -r ".files | map(.fileid) | .[]" | sed "s/\r//"`
#echo "FILEIDs:"
#echo "$fileids"

#Delete all old files
#lines are separated by LF
IFS='
'
echo "Delete all existing files..."
for fileid in $fileids; do
    echo "Deleting id: $fileid"
    url="${baseURL}${fileid}/destroy?accesstoken={${accesstoken}}"
    res=`curl --silent -X POST ${url}`
    checkResultForError "$res"
done

function doUpload {
    path="$1"
    filename=$(basename "${path}")

    echo "Uploading ${filename}..."

    url="${baseURL}create?accesstoken={${accesstoken}}"
    data="{\"filename\":\"${filename}\"}"
    res=`curl --silent --data "$data" -X POST $url`
    #echo $res
    checkResultForError "$res"
    uploadURL=`echo $res | ./jq -r ".upload.puturl"`
    #echo $uploadURL
    res=`curl --silent --upload-file $path $uploadURL`
    #echo "$res"
    checkResultForError "$res"
}

for path in ../build/releases/*; do
    if [ -f "$path" ]; then
        doUpload $path
    fi
done
