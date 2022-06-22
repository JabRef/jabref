# CSL Style Quality Control

We love the CSL style repository, and wish to keep it in tip-top shape. 
Because we have thousands of styles and hundreds of contributors, we rely heavily on automated quality control and periodic maintenance. 
This page describes our best practices.

## Forks and Pull Requests

The "master" branch of the style repository stores the styles for the most recent CSL release. 
To allow changes to be reviewed before they end up in "master", we try to avoid committing changes directly to "master" as much as possible.

Instead, we rely heavily on forks, branches, and pull requests. 
Only our maintainers have direct commit access to the repository. 
Contributors can fork the repository, make their changes in their copy of the repository (ideally in a branch), and create a pull request. 
This allows us to review your changes, and merge them into the official repository.

## Travis CI

[Travis CI](https://travis-ci.org/) is a Continuous Integration service that runs a series of checks on every commit made to the "master" branch, and on every commit in incoming pull requests. 
Each Travis test run is called a "build".

If Travis doesn't detect any problems in your pull request, it will report that the build passed. 
Otherwise the build fails, and you will find a link to the build report that shows which tests failed. 
Add a new commit to your pull request, and Travis will start a new build and run all tests again. 
Only pull requests that pass all the Travis tests can be accepted into the repository.

The tests are stored in the style repository itself. 
Among other things, they make sure that:

* there are no styles with the same file name, title, or ISSN
* all styles are named correctly, and have style IDs and "self" links matching the file names
* all styles validate against the CSL schema
* all styles have the correct license
* all "template" and "independent-parent" links point to existing independent styles
* all style macros are defined and used
* the root directory contains all independent styles, and the "dependent" subdirectory all dependent styles

It is possible to [run the tests locally](#Test-Environment) on your computer, which is especially useful to frequent contributors.

## Indentation, Ordering, and Escaping

To keep styles consistently formatted, we manually run a [Python script](https://github.com/citation-style-language/utilities/blob/master/csl-reindenting-and-info-reordering.py) every few weeks or so. 
The script does the following:

* reindent the XML code, using 2 spaces per indentation level
* put the elements in the `</info>` section in a standard order
* escape characters that are hard to identify by eye (such as the various dashes and spaces)

## Extra-strict Validation

Every so often, we also manually run a [bash script](https://github.com/citation-style-language/utilities/blob/master/style-qc.sh) that validates the styles against a [customized CSL schema](https://github.com/citation-style-language/schema/blob/master/csl-repository.rnc) that is extra strict, and includes requirements specific to our repository. 

## Repairing Problematic Code Patterns

_Here we keep track of problematic CSL code patterns we have observed in the wild, and provide information on how they can be detected and corrected._

### Superscript on citation-number instead of whole citation

In numeric styles where superscripted numbers are used, it's important to superscript the entire citation so that any punctuation is also superscripted, and e.g. use:

```xml
<layout delimiter="," vertical-align="sup">
  <text variable="citation-number"/>
</layout>
```

instead of:

```xml
<layout delimiter=",">
  <text variable="citation-number" vertical-align="sup"/>
</layout>
```

**History**

2016-10: [#2256](https://github.com/citation-style-language/styles/issues/2256) : 47 hits

### Spaces if second field is flushed

If the second field in the bibliography is flushed, then it should not have a space as a prefix.

**Search patterns**

    <bibliography[^>]*second-field-align="flush"[^>]*>.*<layout[^>]*>\r\n\s*<text variable="citation-number"[^>]*/>\r\n\s*<text[^>]*prefix=" "

**Fix**

Delete `prefix=" "` by hand, but it seems possible to automatically delete the last prefix attribute in this pattern. 
No, critical case found. 
Moreover, no case with a different prefix beginning with a space found.

**History**

2015-01: [#1349](https://github.com/citation-style-language/styles/pull/1349) and [#1346](https://github.com/citation-style-language/styles/pull/1346) : ca. 39 matches


### Adjacent spaces from suffix and prefix

**Search Pattern**

    <[^/>]*suffix="[^"/>]* "[^/>]*/?>\s*\r\n\s*<[^/>]*prefix=" [^"/>]*"[^/>]*/?>

**Fix**

Manually look at every case and either 
  (i) delete the space in suffix, 
  (ii) delete the space in the prefix, 
  (iii) use a group with `delimiter=" "`, or 
  (iv) rewritte some parts of the style. 
This can take some time in order to not change the punctation. 
It may be possible to restrict the search pattern more, in order to obtain smaller sets of the same/similar replacements.

**History**

2015-01: [#1301](https://github.com/citation-style-language/styles/issues/1301) : ca. 300 hits in 230 files


## Test Environment

[![Build Status](https://secure.travis-ci.org/citation-style-language/styles.png?branch=master)](http://travis-ci.org/citation-style-language/styles)

To maintain the quality of the styles in the official repository, we have set up an environment for quality-control testing in the repository's [master branch](https://github.com/citation-style-language/styles/tree/master). 
After every commit to this branch, the tests will be executed on [Travis CI](http://travis-ci.org/#!/citation-style-language/styles) in order to alert us should new (or newly updated) styles break any of the quality control rules.

If you are a style author, maintainer or would like to contribute to an existing style, you are advised to install the test environment on your computer in order to run the tests while you're working on a style and to make sure all tests are still passing before you commit any changes to the repository.

### Installation

Before installing the test environment, please make sure that Ruby is available on you computer. 
If Ruby is not installed, please follow the [official instructions](http://www.ruby-lang.org/en/downloads/) on how to install it on your operating-system. 
The test environment should work on all current releases of Ruby and is backwards compatible with version 1.8.7. 
Some of our tests involve RelaxNG schema validation; these tests are based on [libxml](http://www.xmlsoft.org/) via [Nokogiri](http://nokogiri.org/). 
Please see these [operating-system specific instructions](http://nokogiri.org/tutorials/installing_nokogiri.html) if you have any problems installing the test setup because of these requirements.

**Note: it seems in Ruby 1.8.7 you can get some failures with [] and {} comparison failing**

Once Ruby is installed on your computer, it is easy to setup the test environment. 
First, clone into the official repository (if you have previously cloned the repository you can skip this step):

    $ git clone https://github.com/citation-style-language/styles.git
    $ cd styles

You should work directly on the master branch.

Next, we will install all requirements using [Bundler](http://gembundler.com/) (run `[sudo] gem install bundler` to make sure you have the latest version installed). 
Please note that depending on how Ruby is installed on your system you might need administrator privileges to install Ruby Gems, therefore, we have prefixed the commands with an optional `[sudo]`:

    $ [sudo] bundle install

### Usage

Once your bundle is installed there are two ways to run the tests. 
You can use rake:

    $ rake

Or you can run the tests using `rspec`:

    $ bundle exec rspec spec

The latter is useful if you would like to pass special parameters. 
For example, if your Terminal does not support colors, the output may be illegible. 
In this case try running the tests with:

    $ bundle exec rspec spec --no-color

Or, if you would like a more verbose output of all tests, you can switch to a different format, for example:

    $ bundle exec rspec spec --format documentation

Will print a summary of all tests for each style.

#### Testing styles in isolation

With the growing number of styles and tests available in the repository the number of test cases to execute has risen into the range of 100,000 – for obvious reasons, execution may take up to a few minutes on your computer. 
In order to allow for quicker feedback-loops when working on styles, you can set the environment variable CSL_TEST to control which styles to include in the test.

The CSL_TEST variable may contain a list of file names (separated by spaces) of styles to include; 
please note that the name should be the file's full base-name including the '.csl' extension. 
However, for additional flexibility, you can include regular expression wildcards as well.

    $ CSL_TEST="apa.csl vancouver.csl" bundle exec rspec spec

    $ CSL_TEST="chicago.*" bundle exec rspec spec

Finally, you can set the CSL_TEST variable to the special value 'git'; 
by doing that you can limit the styles to be included in the test run to those styles which are currently marked as modified in your local git repository.

    $ CSL_TEST="git" bundle exec rspec spec

#### Windows

For colored output of the test results on Windows, [see this guide](http://softkube.com/blog/ansi-command-line-colors-under-windows/).
