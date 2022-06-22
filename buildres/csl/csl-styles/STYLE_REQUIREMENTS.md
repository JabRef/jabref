# CSL Style Repository Requirements

Before submitting your style to the CSL style repository, please make sure it follows our requirements:

##### 1 - Title Abbreviations

The style name in `<title/>` should be written out in full. 
To store a title abbreviation, use the `<title-short/>` element, e.g.:

```xml
   <info>
     <title>Modern Humanities Research Association</title>
     <title-short>MHRA</title-short>
   </info>
```

For university department styles (and other institutional styles), always put the name of the institution before the name of the department. 
Institutional styles may also mention the type of style (e.g. "Vancouver", "APA", or "Harvard") in the title. 
Use hyphens to separate these elements, e.g. "Oxford Brookes University - Faculty of Health and Life Sciences - Harvard". 

##### 2 - Title Diacritics

Don't remove diacritics from the style title. 
For example, use `<title>Associação Brasileira de Normas Técnicas</title>` instead of `<title>Associacao Brasileira de Normas Tecnicas</title>`.

##### 3 - Style Locale

If your style is meant to be used in one particular language, set the `default-locale` attribute on `<style/>` to the appropriate locale code. 
For example, CSL styles for English-language journals should typically be set to US English ("en-US") or British English ("en-GB"). 
For a list of languages and their locale codes, see https://github.com/citation-style-language/locales/wiki.

When using `default-locale`, add the chosen locale to the style title, unless the language is English. 
An example:

```xml
   <style xmlns="http://purl.org/net/xbiblio/csl" class="in-text" version="1.0" default-locale="de-DE">
     <info>
       <title>Zeitschrift für Soziologie (Deutsch)</title>
       <id>http://www.zotero.org/styles/zeitschrift-fur-soziologie</id>
     </info>
   </style>
```

##### 4 - File Name

The file name of a style should be based on its title. 
However, file names may only contain lowercase roman letters (a-z), digits (0-9), and single hyphens (-), and must end with the ".csl" extension. 
To create the file name from the title:

  * replace capitals with lowercase letters
  * replace ampersands with "and" (e.g., "Arts & Health" becomes "arts-and-health.csl")
  * replace spaces and apostrophes with hyphens (e.g., "Documents d'archéologie française (French)" becomes "documents-d-archeologie-francaise.csl")
  * drop diacritics (e.g., "für" becomes "fur", not "fuer")
  * drop text between parentheses (e.g., "Ugeskrift for Læger (Danish)" becomes "ugeskrift-for-laeger.csl")
  * avoid abbreviations (e.g., use "modern-humanities-research-association.csl" instead of "mhra.csl")
  * add the ".csl" extension

You can use a 'slugify' tool like https://blog.tersmitten.nl/slugify/ to quickly lowercase titles and replace spaces by hyphens, although you may have to make some more changes by hand.

##### 5 - Style ID

The style ID must be "http://www.zotero.org/styles/file-name", with "file-name" representing the style's file name without the ".csl" extension. 
For example, the style ID would be "http://www.zotero.org/styles/modern-humanities-research-association" for "modern-humanities-research-association.csl":

```xml
   <info>
     <id>http://www.zotero.org/styles/modern-humanities-research-association</id>
   </info>
```

##### 6 - "self" Link

The style's "self" link, which tells where the style will available online, must be "http://www.zotero.org/styles/file-name", with "**file-name**" representing the style's file name without the ".csl" extension. 
For example, "http://www.zotero.org/styles/modern-humanities-research-association" for "modern-humanities-research-association.csl":

```xml
   <info>
     <link href="http://www.zotero.org/styles/modern-humanities-research-association" rel="self"/>
   </info>
```

##### 7 - License

The style must be licensed under the Creative Commons Attribution-ShareAlike 3.0 License. 
Use the exact text below, without any hard line breaks for ``<rights/>``:

```xml
   <info>
     <rights license="http://creativecommons.org/licenses/by-sa/3.0/">This work is licensed under a Creative Commons Attribution-ShareAlike 3.0 License</rights>
   </info>
```

##### 8 - "template" Link

If you started from another CSL style, delete the original style authors and contributors, and point to the original style with a "template" link:

```xml
   <info>
     <link href="http://www.zotero.org/styles/original-style" rel="template"/>
   </info>
```

##### 9 - ISSN and eISSN

Journal styles should list the journal's print ISSN (``<issn/>``) and online ISSN (``<eissn/>``), if available:

```xml
   <info>
     <issn>0028-0836</issn>
     <eissn>1476-4687</eissn>
   </info>
```

##### 10 - "documentation" Link

Independent styles should have a "documentation" link that points to a description of the style's citation format. 
For journals, this is typically the "instructions to authors" webpage. 
If a style guide is only available in print, provide a URL that allows us to locate a paper copy.

```xml
   <info>
     <link href="http://www.mhra.org.uk/Publications/Books/StyleGuide/download.shtml" rel="documentation"/>
   </info>
```

##### 11 - XML Indentation

Indent the style's XML with 2 spaces per level. 
Some text editors support automatic indentation of XML. 
Alternatively, use our [style formatter](http://formatter.citationstyles.org/) tool.

##### 12 - Validation

Make sure your finished style [validates](https://github.com/citation-style-language/styles/blob/master/STYLE_DEVELOPMENT.md#validation) against the CSL schema.

That's it! You're ready to [submit](https://github.com/citation-style-language/styles/blob/master/CONTRIBUTING.md) your style!
