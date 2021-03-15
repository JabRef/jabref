# CSL Style Development

CSL relies on volunteers to develop, edit, and maintain its library of citation styles. 
Below are guidelines and tips for editing and developing CSL styles.


## Editing Styles

### CSL Visual Editor

To edit styles, you can use the [Visual CSL Editor](http://editor.citationstyles.org/about/), developed in a cooperation between Mendeley and Columbia University Library. 
See the [CSL Visual Editor user guide](https://github.com/citation-style-editor/csl-editor/wiki/User-guide-for-the-CSL-Editor). 
If you have questions about editing styles with the CSL Visual Editor, you can still ask for help on the [Zotero forums](https://zotero.org/forum) or on the [CSL Discourse page](https://discourse.citationstyles.org/). 
Please only use the [Issues page of this repository](https://github.com/citation-style-language/styles/issues) to report bugs, not to ask general style editing questions.


### Manually editing CSL styles

You can also edit CSL styles manually. 
Zotero includes a [built-in CSL Style Editor](https://zotero.org/support/dev/citation_styles/reference_test_pane) that allows you to see how your edits change style output in real time.

You can also edit CSL styles in any plain text editor (e.g. Notepad on Windows or TextEdit on macOS). 
Note that text editors with XML support can be very useful by offering features like syntax highlighting and real-time validation. 
Popular choices are [Atom Editor](https://atom.io/), [VS Code](https://code.visualstudio.com/), the [oXygen XML Editor](https://www.oxygenxml.com/), [Emacs in nXML mode](https://www.thaiopensource.com/nxml-mode/), and [jEdit](https://www.jedit.org/). 

Currently, most documentation for editing CSL styles can be found at:

* http://citationstyles.org/citation-style-language/documentation/


### 1 - Start from the Right Style

If you want to improve an existing CSL style, make sure that you start from the most recent version. 
The most recent version of each CSL style can be found in the [`master` branch of this repository](https://github.com/citation-style-language/styles).

If you want to create a new style, find an existing style that closely matches what you need using the previews in the style repository. 
Typically the best way to find a most similar style is the ["seach by example"](http://editor.citationstyles.org/searchByExample/) function of the CSL Visual Editor. 


### 2 - Edit the Style

Download the style you want to edit to your computer, and open it your plain text editor. 
Make any edits and save your style. 
Be sure that the new file has a `.csl` file extension (you can generally do this by simply typing ”.csl” after the name of your file).

If you are using the Zotero CSL Editor, be sure to save your edits often using the "Save" button to avoid losing your changes.

Refer to the [CSL specification](http://citationstyles.org/downloads/specification.html) for information on the various options available in CSL styles. 
Below, we discuss a few common style edits to get you started.

#### Changing punctuation

In this example, we want to display the publisher ("CSHL Press") and the location of the publisher ("Cold Spring Harbor, NY") in a bibliographic entry. 
While this can be achieved with the code:

```xml
<text variable="publisher"/>
<text variable="publisher-place"/>
```

this would result in "CSHL PressCold Spring Harbor, NY". 
Fortunately, we can add some punctuation with the `prefix`, `suffix`, and `delimiter` attributes. 
Let's say we want to separate the `publisher` and `publisher-place` by a comma-space, and wrap the whole in parentheses, i.e. "(CSHL Press, Cold Spring Harbor, NY)". 
This can be done with:

```xml
<group delimiter=", " prefix="(" suffix=")">
  <text variable="publisher"/>
  <text variable="publisher-place"/>
</group>
```

The advantage of use a `group` element is that whenever you have a `publisher`, but no `publisher-place`, you don't end up with incorrect punctuation: the output would become "(CSHL Press)". 
If you would set the punctuation directly onto the `text` elements, e.g.:

```xml
<text variable="publisher" prefix="("/>
<text variable="publisher-place" prefix=", " suffix=")"/>
```

you would lose the closing bracket, i.e. "(CSHL Press".

#### Changing et-al abbreviation

There are two main settings for et-al abbreviation (e.g., rendering the names "Doe, Smith & Johnson" as "Doe et al."). 
The minimum number of names that activates et-al abbreviation, and the number of names shown before "et al.".

In CSL, these settings can appear on the `style`, `citation`, `bibliography` or `names` elements in the form of the `et-al-min` and `et-al-use-first` attributes (it is possible to have separate settings for items that have been cited previously by using the `et-al-subsequent-min` and `et-al-subsequent-use-first` attributes).

For example,

```xml
<citation et-al-min="3" et-al-use-first="1">
  ...
</citation>
```

will result in name lists like "Doe", "Doe & Smith" and, if there are three or more names, "Doe et al.". 
Try changing these numbers and observe the effect.

#### Changing disambiguation rules

CSL offers multiple methods to disambiguate cites or names. 
For example, a style might normally render only the family name (e.g., "(Doe 1999, Doe 2002)"). 
If the authors are Jane Doe and Thomas Doe, these names can be disambiguated by adding initials or the full given names (e.g., "(J. Doe 1999, T. Doe 2002)").

Disambiguation methods are selected on the `citation` element. 
For example, to disable [given name disambiguation](https://zotero.org/support/kb/given_name_disambiguation), delete the `disambiguate-add-givenname` attribute, e.g., change:

```xml
<citation disambiguate-add-givenname="true">
  ...
</citation>
```

to:

```xml
<citation>
  ...
</citation>
```

#### Changing author separation

By default, several authors are separated by a delimiter `, ` and the word `and`. 
This settings can be changed, for example to use the symbol `&` instead:

```xml
<names variable="author">
  <name form="short" and="symbol" delimiter=", "/>
  ...
</names>
```

or to not use `and` at all, but to use the delimiter `/`:

```xml
<names variable="author">
  <name form="short" delimiter="/"/>
  ..
</names>
```

#### Conditional rendering (full footnote style)

The appearance of citations in (full) footnote styles may depend on their position in the paper. 
If the same source is cited twice, it may be that a shortened version is used in the second (and any further) citation. 
To handle this distinction, one can use [conditional rendering based on the position](http://citationstyles.org/downloads/specification.html#choose) of the citation. 
A generic structure could then look like:

```xml
<citation>
  <layout>
    <choose>
      <if position="ibid-with-locator">
        ...
      </if>
      <else-if position="ibid">
        ...
      </else-if>
      <else-if position="subsequent">
        ...
      </else-if>
      <else>
        ...
      </else>
    </citation>
  </layout>
</citation>
```

If a case is missing in your style, you can add a new case and specify how information should be rendered in that case (e.g. see [Chicago (full note)](https://www.zotero.org/styles/chicago-fullnote-bibliography?source=1) for an example).

#### Note: Updating from Older CSL Versions

You should alway begun editing from a style that uses the current version of CSL. 
If you are starting from an older version of CSL (e.g. CSL 0.8.1), you should first [upgrading it to the current CSL version](http://citationstyles.org/downloads/upgrade-notes.html#updating-csl-0-8-styles) to take advantage of the newest CSL features.

CSL 0.8.1 and 1.0 styles can be easily distinguished by looking at the ```<style/>``` element at the top of the style. 
CSL 1.0 styles include a ```version``` attribute on this element with a value of "1.0", e.g. ```<style xmlns="http://purl.org/net/xbiblio/csl" class="in-text" version="1.0">```. 
CSL 0.8.1 styles lack this attribute. 


### 3 - Change the Style Title and ID

**Important:** Before installing your edited style in your reference manager or otherwise using it, you must change the style title and ID at the top of the style code. 
If you don't change these, your reference manager may overwrite your modified style the next time the original style is updated.

The style title and ID are stored within the `<title/>` and `<id/>` elements near the top of the style. 
For example:

```xml
<title>American Psychological Association 6th edition</title>
<title-short>APA</title-short>
<id>http://www.zotero.org/styles/apa</id>
```

can be changed to:

```xml
<title>American Psychological Association 6th edition - Modified</title>
<title-short>APA - modified</title-short>
<id>http://www.zotero.org/styles/apa-modified</id>
```


## Validation

We rely heavily on validation against the CSL schema to make sure all CSL styles and [locale files](https://github.com/citation-style-language/locales/wiki) are of sufficient quality. 
Only styles and locales that are valid XML and conform to the CSL schema can be expected to work correctly with all CSL-compatible applications.

### CSL Validator

To validate your style or locale file, please use our own [CSL Validator](http://validator.citationstyles.org/). Styles and locales can be selected via URL, file upload, or copy and paste into a text field.

### Alternative validators

If the [CSL Validator](http://validator.citationstyles.org/) website is down or doesn't work, you can try one of the validators below.

#### csl-validator.js

CSL styles and locale files can also be validated at [](http://simonster.github.io/csl-validator.js/). 
Just paste in your style or locale and click the "Validate" button to validate against the CSL 1.0.1 schema. 
If there aren't any validation errors you will get the message "Validation successful.".

#### Validator.nu

You can also use [Validator.nu](http://validator.nu/?schema=https%3A%2F%2Fgithub.com%2Fcitation-style-language%2Fschema%2Fraw%2Fv1.0.1%2Fcsl.rnc&parser=xml&laxtype=yes&showsource=yes):

  - Select your style or locale. 
    You can provide an URL if your style/locale is accessible online (the "Address" option), select the file on your computer ("File Upload"), or copy and paste the complete code into the text box ("Text Field").
  - (pre-selected if using the link above) 
    Enter the URL to the CSL schema in the "Schemas" text field. 
    For CSL 1.0.1, this is https://github.com/citation-style-language/schema/raw/v1.0.1/csl.rnc. 
    To validate styles in the older CSL 1.0 and 0.8.1 formats, use https://github.com/citation-style-language/schema/raw/v1.0/csl.rnc or https://github.com/citation-style-language/schema/raw/v0.8.1/csl.rnc, respectively.
  - (pre-selected if using the link above) 
    In the "Parser" drop-down menu, select the "XML; don't load external entities" option. 
    Also check the "Be lax about HTTP Content-Type" check-box.
  - Click the “Validate” button. 
    If the style/locale is valid, you will get the message “The document validates according to the specified schema(s) and to additional constraints checked by the validator.”, or, if the style/locale is invalid, "There were errors.". 
    Warnings can be ignored; only errors are important.

#### Local Validators

You can also validate your style/locale on your own computer with any tool that offers XML validation and supports the RELAX NG Compact schema language. 
Examples are the [Atom](https://atom.io/) with the [atomic-csl plugin](https://github.com/bdarcus/atomic-csl), [Emacs](http://www.gnu.org/software/emacs/) with the [nXML addon](http://www.thaiopensource.com/nxml-mode/), [jEdit](http://www.jedit.org/) with the XML addon and the [CSL schema](https://github.com/citation-style-language/schema/zipball/v1.0), and the command-line utilities [Jing](http://www.thaiopensource.com/relaxng/jing.html) and [RNV](http://www.davidashen.net/rnv.html).


## Quality Control

We strive to keep the CSL Style Repository in tip-top shape. 
Becuase we have thousands of styles and hundreds of contributors, we rely heavily on automated quality control and periodic maintenance. 
Please see [Quality Control](https://github.com/citation-style-language/styles/blob/master/QUALITY_CONTROL.md) for our best practices.


## Sharing Styles

If you think that your modified style might be useful to other people, consider [submitting](https://github.com/citation-style-language/styles/blob/master/CONTRIBUTING.md) it to the CSL Style Repository.

You can also host CSL styles on your own website.


## Getting Help

If you have questions about editing CSL styles, you can still ask for help on the [Zotero forums](https://zotero.org/forum) or on the [CSL Discourse page](https://discourse.citationstyles.org/). 
Please only use the [Issues page of this repository](https://github.com/citation-style-language/styles/issues) to report bugs, not to ask general style editing questions.
