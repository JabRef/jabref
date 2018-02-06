package org.jabref.performance;

public class BibtexEntryGenerator {

    public String generateBibtexEntries(int number) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < number; i++) {
            sb.append(generateBibtexEntry(i));
            sb.append("\n");
        }
        return sb.toString();
    }

    private String generateBibtexEntry(int i) {
        return "@article{einstein1916grundlage" + i + ",\n" +
                "  title={Die grundlage der allgemeinen relativit{\\\"a}tstheorie},\n" +
                "  author={Einstein, Albert},\n" +
                "  journal={Annalen der Physik},\n" +
                "  volume={354},\n" +
                "  number={7},\n" +
                "  pages={769--822},\n" +
                "  year={1916},\n" +
                "  publisher={Wiley Online Library}\n" +
                "}\n";
    }

}
