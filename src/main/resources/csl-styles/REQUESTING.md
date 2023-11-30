# Requesting CSL Styles

Below are instructions to request new CSL styles and to report problems in existing styles.

We only have a few volunteers who respond to incoming requests, so please follow our instructions closely to make the process more efficient. 
While we typically work for free, we do charge for, e.g., creating styles for universities or departments ([contact us](http://citationstyles.org/contact/) for pricing). 
You can also try to [edit CSL styles](https://github.com/citation-style-language/styles/blob/master/STYLE_DEVELOPMENT.md#editing-styles) yourself.

## Requesting a New Style

1. First, make sure the style you're requesting isn't already available. 
   The easiest way to check this is to search the [Zotero Style Repository](http://www.zotero.org/styles), which has all our styles.
2. If we don't have the style, try a quick search of the [Zotero forums](http://forums.zotero.org/) to see if there is already a Zotero forum thread for the style you want to request. 
   If there is an existing thread, check whether you can provide any missing information that we might need to create the style. 
   The thread might also contain (technical) reasons why we currently cannot support your style.
3. If you can't find an existing Zotero forum thread, create a new one. 
   **If you have already started a thread, just update it; do not create a new one.** 
   Name your post "Style Request: [name of style]", and include the following information:

    * A link to online style documentation (e.g. for a journal, this would generally be a link to the journal's "Instructions to Authors" section). 
    * For journals, the journal's ISSN (print version) and/or e-ISSN (online version). 
      If you can't find this information on the journal website, try looking up the journal in the [NLM Catalog](http://www.ncbi.nlm.nih.gov/nlmcatalog).
    * Two citations, for a journal article and a book chapter, in the format of the style you're requesting. 
      **Create these citations for the two items shown below, the article by Campbell and Pedersen and book chapter by Mares.** 
      **If your request does not contain these specific citations, you will be asked to revise it**. 
      Provide both in-text citations and bibliographic entries. 
      For, e.g., the APA style, these citations would look like:

      > In-text citation:  
      > (Campbell & Pedersen, 2007)  
      > (Mares, 2001)
      >
      > Bibliography:  
      > Campbell, J. L., & Pedersen, O. K. (2007). The varieties of capitalism and hybrid success. *Comparative Political Studies*, *40*(3), 307–332. https://doi.org/10.1177/0010414006286542  
      > Mares, I. (2001). Firms and the welfare state: When, why, and how does social policy matter to employers? In P. A. Hall & D. Soskice (Eds.), *Varieties of capitalism. The institutional foundations of comparative advantage* (pp. 184–213). New York: Oxford University Press.  

      | Field               | Value                                                                            |
      |---------------------|----------------------------------------------------------------------------------|
      | Type                | article-journal                                                                  |
      | Title               | The varieties of capitalism and hybrid success                                   |
      | Author              | John L. Campbell, Ove K. Pedersen                                                |
      | Issued              | 2007/3/1                                                                         |
      | Container-title     | Comparative Political Studies                                                    |
      | Volume              | 40                                                                               |
      | Issue               | 3                                                                                |
      | Page                | 307-332                                                                          |
      | URL                 | http://cps.sagepub.com.turing.library.northwestern.edu/content/40/3/307.abstract |
      | DOI                 | 10.1177/0010414006286542                                                         |
      | ISSN                | 1552-3829                                                                        |
      | JournalAbbreviation | Comp. Polit. Stud.                                                               |
      | Language            | en-US                                                                            |
      | Accessed            | 2010/7/26                                                                        |

      | Field           | Value                                                                                   |
      |-----------------|-----------------------------------------------------------------------------------------|
      | Type            | chapter                                                                                 |
      | Title           | Firms and the welfare state: When, why, and how does social policy matter to employers? |
      | Author          | Isabela Mares                                                                           |
      | Editor          | Peter A Hall, David Soskice                                                             |
      | Issued          | 2001                                                                                    |
      | Container-title | Varieties of capitalism. The institutional foundations of comparative advantage         |
      | Page            | 184-213                                                                                 |
      | Publisher       | Oxford University Press                                                                 |
      | Publisher-place | New York                                                                                |
      | Event-place     | New York                                                                                |
      | ISBN            | 9780199247752                                                                           |
      | Language        | en-US                                                                                   |

    * Finally, if possible, also provide a link to a freely available paper formatted with the style you're requesting. 
      Published papers often help clarify formatting requirements not discussed in the style guide. 
      For journals that aren't open access, you can often find a free sample issue, or you maybe be able to find a freely available PDF of a recent journal article via, e.g., Google Scholar.

P.S. Instead of requesting a new style, you can also look for an existing CSL style that has a format identical or similar to what you're looking for. 
You can do this with our [CSL style editor](http://editor.citationstyles.org/). 
Visit the “Search by Example” tab, and change one of the example references into the desired format (using the metadata of the selected item). 
Click “Search”, and the editor will show you the CSL styles that most closely match the format you provided. 
See also the [CSL editor user guide](https://github.com/citation-style-editor/csl-editor/wiki/User-guide-for-the-CSL-Editor). 

### Dependent Styles

Please tell us if we already have a CSL style with the format you're looking for, but with a different style name. 
For instance, journals from the same publisher (e.g. "Nature" and "Nature Biotechnology") often use the same style format. 
Other journals simply use one of the main style guides, such as APA.

In these cases, we can create a *dependent style*. 
These dependent styles (e.g. the CSL style for "Nature Biotechnology") simply point to a regular *independent style* with the desired style format (e.g. the CSL style for "Nature"). 
Dependent styles don't define a style format themselves and are much easier to create than independent styles.

## Reporting Style Errors

Requesting changes to existing CSL styles is very similar to requesting new styles. 
However, in this case, there will often already be a Zotero forum thread for your style of interest. 
Before posting, please make sure you have the most recent version of the style installed. 
In your post, give examples of how the existing CSL style format should change, and include the relevant excerpt from the style guidelines, or just give a link to the guidelines.
