# Switch to our new repo

1. Rename your old local repo directory to jabref_backup
2. Clone new repo with 
   - HTTPS: `git clone https://github.com/josphstar/jabref.git`
   - SSH: `git clone git@github.com:josphstar/jabref.git`
3. Backup the .idea folder somewhere else outside the repo directory
4. Delete the .idea folder from the new repo and copy the .idea folder from the old repo to the new one.
5. Start IntelliJ with the build.gradle in the new repo
6. `git remote add upstream git@github.com:JabRef/jabref.git`
