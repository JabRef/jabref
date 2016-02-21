For text inspirations, consider [How to write the perfect pull request](https://github.com/blog/1943-how-to-write-the-perfect-pull-request).

### Checklist for a pull request

Please check that you did following tasks.
Remove them after completion, then submit the pull request.

- [ ] ensure that your source branch is *not* master, but something else
- [ ] ensure that you added your change to CHANGELOG.md
- [ ] ensure that you followed [the formal requirements on a pull request](https://github.com/JabRef/jabref/blob/master/CONTRIBUTING.md#formal-requirements-for-a-pull-request)
- [ ] tests are green
- [ ] changes explained briefly in pull request message. It is OK if you copy from commit messages.
  - [ ] What is the problem addressed?
  - [ ] What gets better? Why?
  - [ ] What other solutions have been considered and dropped?
  - [ ] Example template (not mandatory): `In the context of <use case/user story u>, facing <concern c>, we decided for <option o> and neglected <other options>, to achieve <system qualities/desired consequences>, accepting <downside d/undesired consequences>, because <additional rationale>.`
  - Recommended reading: http://www.infoq.com/articles/sustainable-architectural-design-decisions
- [ ] check the quality of your pull request at https://www.codacy.com/app/simonharrer/jabref/pullRequests
