---
parent: Code Howtos
---
# XMP Parsing

Example XMP metadata from a PDF file (src/test/resources/org/jabref/logic/importer/fileformat/pdf/2024_SPLC_Becker.pdf):

```xml
<?xpacket begin="﻿" id="W5M0MpCehiHzreSzNTczkc9d"?>
<x:xmpmeta xmlns:x="adobe:ns:meta/">
  <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
    <rdf:Description rdf:about="" xmlns:dc="http://purl.org/dc/elements/1.1/">
      <dc:format>application/pdf</dc:format>
      <dc:identifier>doi:10.1145/3646548.3672587</dc:identifier>
    </rdf:Description>
    <rdf:Description rdf:about="" xmlns:prism="http://prismstandard.org/namespaces/basic/2.1/">
      <prism:doi>10.1145/3646548.3672587</prism:doi>
      <prism:url>https://doi.org/10.1145/3646548.3672587</prism:url>
    </rdf:Description>
    <rdf:Description rdf:about="" xmlns:crossmark="http://crossref.org/crossmark/1.0/">
      <crossmark:MajorVersionDate>2024-09-02</crossmark:MajorVersionDate>
      <crossmark:CrossmarkDomainExclusive>true</crossmark:CrossmarkDomainExclusive>
      <crossmark:CrossMarkDomains>
        <rdf:Seq>
          <rdf:li>dl.acm.org</rdf:li>
        </rdf:Seq>
      </crossmark:CrossMarkDomains>
      <crossmark:DOI>10.1145/3646548.3672587</crossmark:DOI>
    </rdf:Description>
    <rdf:Description rdf:about="" xmlns:pdfx="http://ns.adobe.com/pdfx/1.3/">
      <pdfx:CrossMarkDomains>
        <rdf:Seq>
          <rdf:li>dl.acm.org</rdf:li>
        </rdf:Seq>
      </pdfx:CrossMarkDomains>
      <pdfx:CrossmarkDomainExclusive>true</pdfx:CrossmarkDomainExclusive>
      <pdfx:doi>10.1145/3646548.3672587</pdfx:doi>
      <pdfx:CrossmarkMajorVersionDate>2024-09-02</pdfx:CrossmarkMajorVersionDate>
    </rdf:Description>
  </rdf:RDF>
</x:xmpmeta>
<?xpacket end="w"?>
```

`org.apache.xmpbox.xml.DomXmpParser` cannot ignore unknown namespaces. Therefore, we need to exact the known elements.
