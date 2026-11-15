---
parent: Requirements
---
# Groups

## Automatic keyword group must offer subgroup for entries without value
`req~groups.automatic-keyword.missing-value-subgroup~1`

An automatic group based on a keyword field (for example, `ranking`) lists one subgroup per value.
Entries that have no value in that field, or only delimiters, appear in an additional subgroup "No value", so they can be found without a manual search.
The subgroup is only shown when at least one entry has no value.

See <https://github.com/JabRef/jabref/issues/10264>.

Needs: impl, utest

<!-- markdownlint-disable-file MD022 -->
