# JabRef Developer Documentation

The developer documentation is created using the Jekyll Theme [Just the Docs](https://just-the-docs.github.io/just-the-docs/).

## Local Development

For local development, follow the [Jekyll installation instructions](https://jekyllrb.com/docs/installation/).
Installing the latest version of ruby followed by `gem install bundler` should be enough.

Afterwards, run

```terminal
bundle install
jekyll serve --livereload
```

and go to <http://localhost:4000/> in your browser.

On **Windows**, using a dockerized environment is recommended:

```terminal
docker run -p 4000:4000 --rm --volume="C:\git-repositories\jabref\docs":/srv/jekyll jekyll/jekyll:4 jekyll serve
```

In case you get errors regarding `Gemfile.lock`, just delete `Gemfile.lock` and rerun.
