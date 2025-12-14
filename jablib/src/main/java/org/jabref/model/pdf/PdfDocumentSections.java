package org.jabref.model.pdf;

import java.util.List;
import java.util.Optional;

public record PdfDocumentSections(
        String fullText,
        List<PdfSection> sections,
        int pageCount
) {
   private static final List<String> CITATION_RELEVANT_SECTIONS = List.of(
           "related work",
           "literature review",
           "background",
           "previous work",
           "state of the art",
           "related studies",
           "theoretical background",
           "prior work"
   );

   public PdfDocumentSections {
       if (fullText == null) {
           throw new IllegalArgumentException("Full text cannot be null");
       }
       sections = sections != null ? List.copyOf(sections) : List.of();
   }

   public List<PdfSection> getCitationRelevantSections() {
       return sections.stream()
               .filter(section -> CITATION_RELEVANT_SECTIONS.stream()
                       .anyMatch(name -> section.name().toLowerCase().contains(name)))
       .toList();
   }

   public Optional<PdfSection> findSection(String name) {
       String lowerName = name.toLowerCase();
       return sections.stream()
               .filter(section -> section.name().toLowerCase().contains(lowerName))
               .findFirst();
   }

   public boolean hasCitationRelevantSections() {
       return !getCitationRelevantSections().isEmpty();
   }

   public Optional<PdfSection> getReferencesSection() {
       return sections.stream()
               .filter(section -> {
                   String lowerName = section.name().toLowerCase();
                   return lowerName.contains("references") || lowerName.contains("bibliography");
               })
               .findFirst();
   }
}
