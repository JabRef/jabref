# Contributing to the CSL locale repository

We welcome contributions to the Citation Style Language (CSL) locale files in this repository.

The CSL locale files provide the standard translations for automatic localization of CSL styles.
As such, the CSL locale files should generally contain the most commonly used translations for a given locale.
Less common translations can be [provided in the individual CSL styles](https://docs.citationstyles.org/en/stable/specification.html#locale) that require them, which will overwrite the CSL locale file translations.

Because each CSL locale file offers the standard translations for all CSL styles, changes should be made conservatively and carefully.
We will often ask a second native speaker and/or past contributor to look over your proposed changes.

## Licensing and crediting

By creating a pull request, you agree to license your contributions under the [Creative Commons Attribution-ShareAlike 3.0 Unported license](https://creativecommons.org/licenses/by-sa/3.0/) license.

In addition, if you're interested in being credited, please add yourself to the locale file as translator. See the example below (both the `<email/>` and `</uri>` elements are optional):

```xml
<info>
  <translator>
    <name>John Doe</name>
    <email>john.doe@citationstyles.org</email>
    <uri>https://citationstyles.org/</uri>
  </translator>
  <rights license="http://creativecommons.org/licenses/by-sa/3.0/">This work is licensed under a Creative Commons Attribution-ShareAlike 3.0 License</rights>
  <updated>2019-01-01T12:00:00+00:00</updated>
</info>
```
