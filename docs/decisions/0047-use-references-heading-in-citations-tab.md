---
nav_order: 47
parent: Decision Records
status: accepted
date: 2025-07-31
decision-makers: "@ryan-carpenter, @ThiloteE, @SiedlerChr, @callixtus, @koppor"
---
<!-- markdownlint-disable-next-line MD025 -->
# Use References Headings in Citations Tab

## Context and Problem Statement

The tab "Citation relations" shows the references of the paper as well as the papers citing the current paper.
It is layouted using two columns.
When not working deeply with citation relations, it is unclar, what the left column and the right column present.
Before July 2025, JabRef used "cites" and "cited by" as headings, but these were too short.

There is only one form of citation. Citation is always "backward", so there is nothing wrong with cites and cited by, except that sometimes you need a noun to refer to things that are cited or cited by, and there is only one word for that.
It's "citations".
That's a problem when you want to distinguishe between citations that mean cites and citations that mean cited by. Hence the use of forward and backward.

How to name the headings of these two areas?

## Decision Drivers

* Headings should be understandable by (nearly) all user groups
* Headings should be consistent with terms used in certain fields

## Considered Options

* "References cited in {citationkey}" and "References that cite {citationkey}"
* "backward (cites)" and "forward (cited by)"
* "Backward Citations" and "Forward Citations"
* "References (cites)" and "Citations (cited by)"
* "References" and "Cited by"
* "Cites" and "Cited by"

## Decision Outcome

Chosen option: ""References cited in {citationkey}" and "References that cite {citationkey}""", because comes out best (see below).

## Pros and Cons of the Options

## "References cited in {citationkey}" and "References that cite {citationkey}"

{citationkey} - if not available, use "this entry".

Tooltip left: Also called "backward citations"

Tooltrip right: Also called "forward citations"

Regarding "cited in {citationkey}" or "cited by {citationkey}", either would do, but I am going on the theory that user-x thinks of {citationkey} as the paper, so the most natural cognitive process is that the references are cited by the authors in the paper.

* Good, because no confusion regarding "References" and "Citations".
* Good, because left and right cite are different on purpose to create contrast.

## "backward (cites)" and "forward (cited by)"

Tooltip: outgoing citations - works that are cited by this work. "Backward", because looking back in time.

Tooltip: incoming citations - works that cite this work. "Forward", because looking forward in time.

* Good, because all lower case
* Good, because combines two concepts in the heading
* Bad, because uses braces

## "Backward Citations" and "Forward Citations"

Backward citations

Tooltip: Outgoing citations - works that are cited by this work. "Backward", because looking back in time.

Forward citations

Tooltip: Incoming citations - works that cite this work. "Forward", because looking forward on the time axis, with the time the work was created being the dividing line between backwards and forwards.

Technical words should be defined in the tooltip explicitly. Forward and backwards and sideways and upwards and outgoing and incoming are all technical words.

* Good, because common terms in SLR
* Bad, because "backward" and forward" sound too technical
* Bad, because too abstract for the average user and does not have clear semantics

## "References (cites)" and "Citations (cited by)"

References (cites)
Tooltip: Works cited by the work at hand

Citations (cited by)
Tooltip: Works citing the work at hand

* Good, because used by Semantic Scholar
* Good, because combines two concepts in the heading
* Bad, because the braces in the heading are unusual

## "References" and "Cited by"

Example: <https://dblp.org/rec/conf/zeus/VoigtKW21.html>

* Good, because used by DBLP
* Good, because "Cited by" is easy to understand.
* Good, because "Cited by" is also used by Google Scholar
* Bad, because mix of noun and verb

## "Cites" and "Cited by"

* Good, because verbs
* Bad, because too close to each other
