'''Delete local branches which have been merged via GitHub PRs.

Usage (from the root of your GitHub repo):

    delete-squahsed-branches [--dry-run]

If you specify --dry-run, it will only print out the branches that would be
deleted.

This script looks for local branches whose tips have been merged onto the
remote's equivalent of the current branch. This means that you'll probably want
to run it on your master branch or whatever you develop off of. It won't work
well in repos with multiple long-lived branches that you merge onto.

To work with private repos, create a ~/.githubrc file like this:

user.login: your-login
user.token: your-personal-access-token
'''

from glob import glob
import os.path
import re
import subprocess
import sys

from github import Github


DRY_RUN = len(sys.argv) > 1 and sys.argv[1] == '--dry-run'


def get_branch_heads():
    '''Returns a map from branch name --> SHA for local branches.'''
    branch_to_sha = {}

    branches = subprocess.check_output(['git', 'branch']).decode('utf-8').split('\n')

    for branch in branches:
        branch = branch.strip()
        if branch == 'master' or branch == '': continue
        sha = subprocess.check_output(['git', 'rev-parse', branch]).decode('utf-8')
        branch_to_sha[branch] = sha.strip()
    return branch_to_sha


def github():
    '''Returns a GitHub API object with auth, if it's available.'''
    def simple_fallback(message=None):
        if message: sys.stderr.write(message + '\n')
        return Github()

    github_rc = os.path.join(os.path.expanduser('~'), '.githubrc')
    if os.path.exists(github_rc):
        try:
            pairs = open(github_rc).read()
        except IOError:
            return simple_fallback('Unable to read .githubrc file. Using anonymous API access.')
        else:
            kvs = {}
            for line in pairs.split('\n'):
                if ':' not in line: continue
                k, v = line.split(': ', 1)
                kvs[k] = v

            login = kvs.get('user.login')
            if not login:
                return simple_fallback('.githubrc missing user.login. Using anonymous API access.')

            password = kvs.get('user.password')
            token = kvs.get('user.token')

            if password and token:
                raise ValueError('Only specify user.token or user.password '
                                 'in your .githubrc file (got both)')

            auth = token or password

            if not auth:
                return simple_fallback('.githubrc has neither user.password nor user.token.'
                                       'Using anonymous API access.')
            return Github(login, auth)
    else:
        return simple_fallback('.githubrc not found at: %s . Using anonymous API access.' % github_rc)


def get_github_remote():
    '''Returns (org, repo) for the current GitHub repo.'''
    out = subprocess.check_output(['git', 'remote', '-v']).decode('utf-8')
    lines = out.split('\n')
    for line in lines:
        parts = line.split('\t')
        if parts[0] != 'origin': continue
        m = re.search(r'github.com/([^/]+)/([^./ ]+)', parts[1])
        if m:
            return m.group(1), m.group(2)
        else:
            raise ValueError(parts[1])
    raise ValueError('Unable to find GitHub remote')


def get_current_branch():
    '''Returns the name of the current branch (e.g. 'master').'''
    return (subprocess.check_output(
            ['git', 'symbolic-ref', '-q', '--short', 'HEAD'])
            .decode('utf-8').strip())


target_branch = get_current_branch()
if target_branch != 'master':
    warn = '\033[93m'
    clear = '\033[0m'
    message = 'origin/%s%s%s' % (warn, target_branch, clear)
else:
    message = 'origin/master'

sys.stderr.write('Finding local branches which were merged onto %s via GitHub...%s\n' % (
    message, ' (DRY RUN)' if DRY_RUN else ''))


org, repo = get_github_remote()
g = github()
repo = g.get_user(org).get_repo(repo)

merged_shas = [
    pr.head.sha
    for pr in repo.get_pulls(state='closed', base=target_branch)
    ]

for branch, sha in get_branch_heads().items():
    if sha in merged_shas:
        if DRY_RUN:
            print('Would delete local branch %s' % branch)
        else:
            subprocess.check_call(['git', 'branch', '-D', branch])

