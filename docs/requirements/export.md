---
parent: Requirements
---
> [!IMPORTANT]
> This project does not accept fully AI-generated pull requests. AI tools may only be used for assistance. You must understand and take responsibility for every change you submit.
>
> Read and follow:
> • [AGENTS.md](./AGENTS.md)
> • [CONTRIBUTING.md](./CONTRIBUTING.md)

# Export

## Export BIBFRAME 2.0 RDF/XML
`req~export.bibframe.rdfxml~1`

JabRef exports each entry as a linked BIBFRAME 2.0 Work and Instance in striped RDF/XML. Resource identifiers are deterministic for unchanged bibliographic content, unique for duplicate entries within one file, and independent of citation keys.

Needs: impl, utest

<!-- markdownlint-disable-file MD013 MD033 MD041 -->
<!-- markdownlint-disable-file MD022 -->
