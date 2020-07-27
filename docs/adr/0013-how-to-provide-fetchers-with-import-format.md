# How to provide fetchers with import format

## Context and Problem Statement

Fetchers import BibEntries either as BibTeX or BibLaTeX.
To keep databases formatted homogenous, fetchers have to return the requested format.
How should we provide the fetchers with the format information?

## Considered Options

* Pass fetchers the format as a field during initialization
* Pass fetchers the format, for each request

## Decision Outcome

Chosen option: "Pass fetchers the format, for each request", 
because this way less state has to be managed by the fetcher, and less code has to be written.

## Pros and Cons of the Options

### Pass fetchers the format as a field during initialization

* Good, because on fetching, the format does not have to be provided each time
* Bad, because for each format requires a separate fetcher instance.
* Bad, because fetcher keeps the format information as state, and performs the state conversion implicitly
* Bad, because the code to access fetchers has to be changed

### Pass fetchers the format, for each request

* Good, because programmer has to explicitly provide the format each time he calls the method, making the conversion more explicit
* Good, because it links the format information directly to the method that requires it
* Bad, because format has to be passed each time
