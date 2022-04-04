# How We Work ("work in progress")

### Before
- Check for new commits in repos "SE_PROJECT_FILES" and "josphstar/jabref"
- `git pull origin main`
- `git pull upstream main`
  - Alternatively (more control): 
  - `git fetch upstream main`
  - `git merge upstream/main`
- `git checkout WORKING_BRANCH`

### During
- Commit often and after every specific task
  - Commit msg example: `"Add search feature"`
    - Capitalization at the start
    - Imperative mood (Add, Merge, Update...)
    - [Complete Guidelines](https://chris.beams.io/posts/git-commit/)
- How to commit:
  - Commit often and after every specific task
  - Before pushing your commits:
    - `git pull origin main`
    - Resolve any occuring merge conflicts
    - `git push origin BRANCH`

### Code Reviews
- Make notes about the code you are studying and push everything to "SE_PROJECT_FILES"
- Regular code review meetings where team members explain their specific feature to the other members

### Git Branch Structure
- main
  - develop
    - designdoc
    - lucene_syntax
    - gui
    - searchbar
    - ...