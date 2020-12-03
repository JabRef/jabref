# Use OpenCitations API for Citation-Relations Tab in the Entry Editor

## Context and Problem Statement

The JabRef-Software has an entry editor with multiple tabs. One of them is the "Citation Relations" tab, that offers the possibility to explore citation relations. In order to do so, JabRef queries an API, to get the related articles.

## Considered Options

* [Sematischolar](http://api.semantischolar.org/)
* [OpenCitations](https://opencitations.net/index/api/v1)
* [Citationgecko](http://citationgecko.com/)
* [Yewno](https://www.yewno.com/discover)
* [Openknowledgemaps](https://openknowledgemaps.org/)
* [Wikicite](http://wikicite.org/)
* [Scholarcy](https://www.scholarcy.com/)
* [Dimensions.ai](https://www.dimensions.ai/dimensions-apis/)
* [Springernature](https://www.springernature.com/gp/researchers/bookmetrix)
* [Github: adsabs](https://github.com/adsabs/adsabs-dev-api)

## Decision Outcome

Chosen option: OpenCitations, because its easy to use and one receive results in JSON, that is easy to deal with. Furthermore most of the other options are graphical online tools that are not meant to be used as an API.
