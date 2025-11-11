# External libraries

This document lists the fonts, icons, and libraries used by JabRef.

## bst files

```yaml
Project: IEEEtran
Path:    src/main/resources/bst/IEEEtran.bst
URL:     https://www.ctan.org/tex-archive/macros/latex/contrib/IEEEtran/bibtex
License: LPPL-1.3
```

## Fonts and Icons

The loading animation during loading of recommendations from Mr. DLib is created by <http://loading.io/> and is free of use under license CC0 1.0.

```yaml
Id:      material-design-icons.font
Project: Material Design Icons
Version: v1.5.54
URL:     https://materialdesignicons.com/
License: SIL Open Font License, Version 1.1
Note:    It is important to include v1.5.54 or later as v1.5.54 is the first version offering fixed code points. Do not confuse with http://zavoloklom.github.io/material-design-iconic-font/
```

## Libraries

One can generate a file with all library dependencies by using Gradle task `cyclonedxBom`.
It generates the file `build/cyclonedx/bom.xml` and `build/cyclonedx/bom.json`. The file is in the [SBOM Standard](https://cyclonedx.org/).

[SBOM](bom.json)
