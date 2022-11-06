FROM ruby:2.7

ENV LC_ALL C.UTF-8
ENV LANG en_US.UTF-8
ENV LANGUAGE en_US.UTF-8

EXPOSE 4000

WORKDIR /srv/jekyll
COPY . /srv/jekyll

RUN gem install bundler && bundle install

CMD bundle exec jekyll serve -H 0.0.0.0 -t
