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
docker build . -t jrjekyll
docker run -p 4000:4000 -it --rm --volume="C:\git-repositories\jabref\docs":/srv/jekyll jrjekyll jekyll serve -H 0.0.0.0 -t
```

* With <kbd>Ctrl</kbd>+<kbd>C</kbd> you can stop the server (this is enabled by the `-it` switch).
* In case you get errors regarding `Gemfile.lock`, just delete `Gemfile.lock` and rerun.
* The current `Dockerfile` is based on <https://github.com/just-the-docs/just-the-docs/blob/main/Dockerfile>.
  The [Jekyll Docker image](https://github.com/envygeeks/jekyll-docker#jekyll-docker) did not work end of 20222 (because Ruby was too new).
